package com.restonic4.logistics.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EnergyNetworkManager extends SavedData {
    private static final String DATA_NAME = "logistics_energy_networks";

    /*
        STATE
     */

    private final Map<UUID, EnergyNetwork> networks = new HashMap<>();
    private final Map<BlockPos, UUID> posToNetwork = new HashMap<>();

    /*
        SAVING AND LOADING
     */

    public static EnergyNetworkManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(EnergyNetworkManager::load, EnergyNetworkManager::new, DATA_NAME);
    }

    private static EnergyNetworkManager load(CompoundTag tag) {
        EnergyNetworkManager manager = new EnergyNetworkManager();

        ListTag networkList = tag.getList("networks", Tag.TAG_COMPOUND);
        for (Tag t : networkList) {
            EnergyNetwork network = EnergyNetwork.load((CompoundTag) t);
            manager.networks.put(network.getId(), network);
            for (BlockPos pos : network.getMemberPositions()) {
                manager.posToNetwork.put(pos, network.getId());
            }
        }

        return manager;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag networkList = new ListTag();
        for (EnergyNetwork network : networks.values()) {
            networkList.add(network.save());
        }
        tag.put("networks", networkList);
        return tag;
    }

    /*
        NETWORK MANAGEMENT
     */

    private void createNetwork(ServerLevel level, BlockPos pos) {
        EnergyNetwork network = EnergyNetwork.create();
        network.addPosition(pos);
        addNetwork(network);
        posToNetwork.put(pos.immutable(), network.getId());
        tryRegisterLiveNode(level, network, pos);
        network.recalculateRates();
    }

    private void attachToNetwork(ServerLevel level, UUID id, BlockPos pos) {
        EnergyNetwork network = networks.get(id);
        network.addPosition(pos);
        posToNetwork.put(pos.immutable(), id);
        tryRegisterLiveNode(level, network, pos);
        network.recalculateRates();
    }

    private void mergeNetworks(ServerLevel level, BlockPos pos, Set<UUID> networkIds) {
        Iterator<UUID> iter = networkIds.iterator();
        UUID survivorId = iter.next();
        EnergyNetwork survivor = networks.get(survivorId);

        // Add the new position
        survivor.addPosition(pos);
        posToNetwork.put(pos.immutable(), survivorId);

        // Absorb all other networks into survivor
        while (iter.hasNext()) {
            UUID otherId = iter.next();
            EnergyNetwork other = networks.remove(otherId);
            if (other == null) continue;

            for (BlockPos p : other.getMemberPositions()) {
                posToNetwork.put(p, survivorId);
            }

            survivor.mergeFrom(other);

            for (EnergyNode node : other.getLoadedNodes()) {
                node.setEnergyNetwork(survivor);
            }
        }

        survivor.recalculateRates();
        survivor.clampBuffer();
        tryRegisterLiveNode(level, survivor, pos);
    }

    /*
        TOPOLOGY EVENTS
     */

    public void tick(long currentTick) {
        for (EnergyNetwork network : this.getAllNetworks()) {
            network.tick(currentTick);
        }

        setDirty();
    }

    public void onMemberPlaced(ServerLevel level, BlockPos pos) {
        BlockPos immutablePos = pos.immutable();
        Set<UUID> neighborNetworkIds = getNeighborNetworkIds(level, immutablePos);

        if (neighborNetworkIds.isEmpty()) {
            createNetwork(level, immutablePos);
        } else if (neighborNetworkIds.size() == 1) {
            UUID id = neighborNetworkIds.iterator().next();
            attachToNetwork(level, id, immutablePos);
        } else {
            mergeNetworks(level, immutablePos, neighborNetworkIds);
        }

        setDirty();
    }

    public void onMemberRemoved(ServerLevel level, BlockPos pos) {
        BlockPos immutablePos = pos.immutable();

        UUID networkId = posToNetwork.remove(immutablePos);
        if (networkId == null) return;

        EnergyNetwork network = networks.get(networkId);
        if (network == null) return;

        network.removePosition(immutablePos);

        // Collect neighbors that are still in the network after removal.
        List<BlockPos> neighborsInNetwork = new ArrayList<>();
        for (BlockPos neighbor : getCardinalNeighbors(immutablePos)) {
            if (network.containsPosition(neighbor)) {
                neighborsInNetwork.add(neighbor);
            }
        }

        // Case 1: was the only member, network is now empty
        if (neighborsInNetwork.isEmpty()) {
            networks.remove(networkId);
            setDirty();
            return;
        }

        // Case 2: only one neighbor, no split possible
        // The network is still intact, just smaller. Keep the same object.
        if (neighborsInNetwork.size() == 1) {
            network.recalculateRates();
            setDirty();
            return;
        }

        // Case 3: multiple neighbors, check if they're still connected
        // If all neighbors end up in the same component, there was no split.
        Set<BlockPos> remaining = new HashSet<>(network.getMemberPositions());

        Set<BlockPos> visited = new HashSet<>();
        List<Set<BlockPos>> components = new ArrayList<>();

        for (BlockPos start : neighborsInNetwork) {
            if (visited.contains(start)) continue; // already claimed by an earlier component
            Set<BlockPos> component = floodFill(remaining, start);
            components.add(component);
            visited.addAll(component);
        }

        // If only one component, the network is still fully connected, keep it as-is.
        if (components.size() == 1) {
            network.recalculateRates();
            setDirty();
            return;
        }

        long totalMembers = network.getMemberCount();
        long oldBuffer = network.getEnergyBuffer();

        // Split: dissolve the old network and create one new network per component
        networks.remove(networkId);

        for (Set<BlockPos> component : components) {
            EnergyNetwork newNetwork = EnergyNetwork.create();

            // Distribute the buffer proportionally by component size.
            long share = totalMembers > 0 ? (oldBuffer * component.size()) / totalMembers : 0;

            for (BlockPos p : component) {
                newNetwork.addPosition(p);
                posToNetwork.put(p, newNetwork.getId());
            }

            newNetwork.seedBuffer(share);

            // Re-register any loaded BlockEntities in this component.
            for (BlockPos p : component) {
                tryRegisterLiveNode(level, newNetwork, p);
            }

            newNetwork.recalculateRates();
            newNetwork.clampBuffer();
            addNetwork(newNetwork);
        }

        setDirty();
    }

    /*
        CHUNKS
     */

    public void onChunkLoaded(ServerLevel level, Iterable<BlockEntity> blockEntities) {
        long currentTick = level.getGameTime();
        for (BlockEntity be : blockEntities) {
            if (be instanceof EnergyNode node) {
                BlockPos pos = be.getBlockPos().immutable();
                UUID networkId = posToNetwork.get(pos);
                if (networkId != null) {
                    EnergyNetwork network = networks.get(networkId);
                    if (network != null) {
                        network.flagForCatchUp(currentTick);
                        network.registerLoadedNode(pos, node);
                    }
                }
            }
        }
    }

    public void onChunkUnloaded(Iterable<BlockEntity> blockEntities) {
        for (BlockEntity be : blockEntities) {
            if (be instanceof EnergyNode node) {
                BlockPos pos = be.getBlockPos().immutable();
                UUID networkId = posToNetwork.get(pos);
                if (networkId != null) {
                    EnergyNetwork network = networks.get(networkId);
                    if (network != null) {
                        network.unregisterNode(pos);
                    }
                }
            }
        }
    }

    /*
        GETTERS
     */

    public EnergyNetwork getNetworkAt(BlockPos pos) {
        UUID id = posToNetwork.get(pos);
        return id != null ? networks.get(id) : null;
    }

    public Collection<EnergyNetwork> getAllNetworks() {
        return Collections.unmodifiableCollection(networks.values());
    }

    /*
        INTERNAL HELPERS
     */

    private void addNetwork(EnergyNetwork network) {
        if (networks.containsKey(network.getId())) {
            throw new IllegalStateException("This error is so absurdly unlikely to happen, that if it does happen to you, you could say you're Dream 2.0. Contact us because this is wild.");
        }
        networks.put(network.getId(), network);
    }

    private Set<UUID> getNeighborNetworkIds(ServerLevel level, BlockPos pos) {
        Set<UUID> ids = new HashSet<>();
        for (BlockPos neighbor : getCardinalNeighbors(pos)) {
            UUID id = posToNetwork.get(neighbor);
            if (id != null && isEnergyRelevant(level, neighbor)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private boolean isEnergyRelevant(ServerLevel level, BlockPos pos) {
        if (posToNetwork.containsKey(pos)) return true;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnergyNode) return true;

        return level.getBlockState(pos).getBlock() instanceof EnergyConnectable;
    }

    private Set<BlockPos> floodFill(Set<BlockPos> allowedPositions, BlockPos start) {
        Set<BlockPos> result = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (!result.add(current)) continue; // already visited

            for (BlockPos neighbor : getCardinalNeighbors(current)) {
                if (!result.contains(neighbor) && allowedPositions.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return result;
    }

    private void tryRegisterLiveNode(ServerLevel level, EnergyNetwork network, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnergyNode node) {
            network.registerLoadedNode(pos, node);
        }
    }

    private static List<BlockPos> getCardinalNeighbors(BlockPos pos) {
        return List.of(
                pos.north(), pos.south(),
                pos.east(),  pos.west(),
                pos.above(), pos.below()
        );
    }
}
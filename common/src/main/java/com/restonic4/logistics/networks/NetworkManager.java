package com.restonic4.logistics.networks;

import com.restonic4.logistics.events.ServerTickEvents;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.networks.flags.NetworkFlag;
import com.restonic4.logistics.networks.pathfinding.Parcel;
import com.restonic4.logistics.networks.pathfinding.ParcelRenderSyncPacket;
import com.restonic4.logistics.networks.types.ItemNetwork;
import com.restonic4.logistics.registry.NetworkTypeRegistry;
import com.restonic4.logistics.utils.MinecraftUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

// TODO: We need some method on Network to tell the manager when the network has been discarded???? Older networks still appear after merging multiple networks
public class NetworkManager extends SavedData {
    private static final String DATA_NAME = "logistics_networks";

    private final ServerLevel serverLevel;

    private final Map<UUID, Network> networks = new HashMap<>();
    private final Map<BlockPos, Network> nodePositionIndex = new HashMap<>();
    private final Queue<NetworkChange> pendingChanges = new ConcurrentLinkedQueue<>();

    private NetworkManager(ServerLevel serverLevel) {
        this.serverLevel = serverLevel;
    }

    /*
        LIFECYCLE
     */

    public static NetworkManager get(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(
                tag -> NetworkManager.load(tag, serverLevel),
                () -> new NetworkManager(serverLevel),
                DATA_NAME
        );
    }

    public static void register() {
        ServerTickEvents.END.register((server) -> {
            for (ServerLevel level : server.getAllLevels()) {
                NetworkManager.get(level).tick();
            }
        });
    }

    /**
     * TODO: This has hardcoded ItemNetwork logic, this should be abstracted in some way so any network can benefit while maintaining this clean.
     *  - Some listener on each network for global tick tart en end?
     */
    public void tick() {
        applyPendingChanges();

        List<Parcel> allParcels = new ArrayList<>(); // TODO: Remove

        networks.forEach((uuid, network) -> {
            network.tick();
            if (network.isDirty()) {
                setDirty();
                network.cleanDirtyFlag();
            }

            // TODO: Remove
            if (network instanceof ItemNetwork itemNetwork) {
                allParcels.addAll(itemNetwork.getParcels());
            }
        });

        ServerNetworking.sendToAllInLevel(getServerLevel(), new ParcelRenderSyncPacket(allParcels)); // TODO: Remove

        applyPendingChanges();
    }

    /*
        DATA MANAGEMENT
     */

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag networkList = new ListTag();
        for (Network network : networks.values()) {
            networkList.add(network.save());
        }
        tag.put("networks", networkList);
        return tag;
    }

    private static NetworkManager load(CompoundTag tag, ServerLevel serverLevel) {
        NetworkManager manager = new NetworkManager(serverLevel);

        ListTag networkList = tag.getList("networks", Tag.TAG_COMPOUND);
        for (Tag t : networkList) {
            Network network = Network.createFromTag((CompoundTag) t, serverLevel);
            manager.networks.put(network.getUUID(), network);

            for (NetworkNode node : network.getNodeIndex().getAllNodes()) {
                manager.nodePositionIndex.put(node.getBlockPos(), network);
            }
        }

        return manager;
    }

    /*
        OTHER
     */

    private void applyPendingChanges() {
        if (pendingChanges.isEmpty()) return;

        while (!pendingChanges.isEmpty()) {
            NetworkChange change = pendingChanges.poll();
            switch (change.type()) {
                case ADD -> internalOnMemberPlaced(change.node());
                case REMOVE -> internalOnMemberRemoved(change.blockPos());
            }
        }

        setDirty();
    }

    public void onMemberPlaced(NetworkNode node) {
        pendingChanges.add(NetworkChange.add(node));
    }
    public void onMemberRemoved(BlockPos blockPos) {
        pendingChanges.add(NetworkChange.remove(blockPos));
    }

    public void internalOnMemberPlaced(NetworkNode node) {
        Set<Network> neighborNetworks = getNeighborNetworks(node.getBlockPos(), node.getType().networkType());

        if (neighborNetworks.isEmpty()) {
            // Create brand-new network
            Network network = Network.create(node.getType().networkType(), serverLevel);
            network.getNodeIndex().register(node);
            networks.put(network.getUUID(), network);
            nodePositionIndex.put(node.getBlockPos(), network);
        } else if (neighborNetworks.size() == 1) {
            // Attach to existing network
            Network target = neighborNetworks.iterator().next();
            target.getNodeIndex().register(node);
            nodePositionIndex.put(node.getBlockPos(), target);
        } else {
            // Merge all networks into 1
            Iterator<Network> iter = neighborNetworks.iterator();
            Network survivor = iter.next();

            survivor.getNodeIndex().register(node);
            nodePositionIndex.put(node.getBlockPos(), survivor);

            while (iter.hasNext()) {
                Network otherNetwork = iter.next();
                if (otherNetwork == null) continue;

                survivor.mergeDataFrom(otherNetwork);

                List<NetworkNode> toMove = new ArrayList<>(otherNetwork.getNodeIndex().getAllNodes());
                for (NetworkNode otherNode : toMove) {
                    otherNetwork.getNodeIndex().unregister(otherNode);
                    survivor.getNodeIndex().register(otherNode);
                    nodePositionIndex.put(otherNode.getBlockPos(), survivor);
                }

                networks.remove(otherNetwork.getUUID());
            }
        }

        setDirty();
    }

    public void internalOnMemberRemoved(BlockPos blockPos) {
        Network network = getNetworkByBlockPos(blockPos);
        if (network == null) return;

        NetworkNode node = network.getNodeIndex().findByBlockPos(blockPos);
        if (node == null) return;

        network.getNodeIndex().unregister(node);
        nodePositionIndex.remove(blockPos);

        // Collect neighbors that are still in the network after removal.
        List<BlockPos> neighborsInNetwork = new ArrayList<>();
        MinecraftUtils.forEachNeighbor(blockPos, (neighborPos) -> {
            NetworkNode neighborNode = network.getNodeIndex().findByBlockPos(neighborPos);
            if (neighborNode == null) return;

            Network neighborNetwork = neighborNode.getNetwork();
            if (neighborNetwork == null) return;
            if (neighborNetwork.getType() != network.getType()) return;

            neighborsInNetwork.add(neighborPos);
        });

        // Case 1: was the only member, network is now empty
        if (neighborsInNetwork.isEmpty()) {
            if (network.getNodeIndex().getAllNodes().isEmpty()) {
                networks.remove(network.getUUID());
            }
            return;
        }

        // Case 2: only one neighbor, no split possible
        // The network is still intact, just smaller. Keep the same object.
        if (neighborsInNetwork.size() == 1) return;


        // Case 3: multiple neighbors, check if they're still connected
        // If all neighbors end up in the same component, there was no split.
        Set<BlockPos> remaining = network.getNodeIndex().getAllNodes().stream().map(NetworkNode::getBlockPos).collect(Collectors.toSet());

        Set<BlockPos> visited = new HashSet<>();
        List<Set<NetworkNode>> components = new ArrayList<>();

        for (BlockPos start : neighborsInNetwork) {
            if (visited.contains(start)) continue; // already claimed by an earlier component

            Set<BlockPos> componentPositions = floodFill(remaining, start);
            visited.addAll(componentPositions);

            Set<NetworkNode> componentNodes = componentPositions.stream()
                    .map(p -> network.getNodeIndex().findByBlockPos(p))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (!componentNodes.isEmpty()) {
                components.add(componentNodes);
            }
        }

        // If only one component, the network is still fully connected, keep it as-is.
        if (components.size() <= 1) return;

        // Split: dissolve the old network and create one new network per component
        networks.remove(network.getUUID());

        List<Network> newChildren = new ArrayList<>();

        for (Set<NetworkNode> nodeSet : components) {
            Network newNetwork = Network.create(network.getType(), serverLevel);

            for (NetworkNode foundNode : nodeSet) {
                newNetwork.getNodeIndex().register(foundNode);
                nodePositionIndex.put(foundNode.getBlockPos(), newNetwork);
            }

            networks.put(newNetwork.getUUID(), newNetwork);
            newChildren.add(newNetwork);
        }

        network.onSplit(newChildren);

        setDirty();
    }

    public <T extends Network> Optional<T> getAdjacentNetwork(BlockPos pos, Class<T> networkClass) {
        Network self = nodePositionIndex.get(pos);
        if (networkClass.isInstance(self)) {
            return Optional.of(networkClass.cast(self));
        }

        return MinecraftUtils.findNeighbor(pos, neighborPos -> {
            Network candidate = nodePositionIndex.get(neighborPos);
            if (networkClass.isInstance(candidate)) {
                return networkClass.cast(candidate);
            }
            return null;
        });
    }

    private Set<BlockPos> floodFill(Set<BlockPos> allowedPositions, BlockPos start) {
        Set<BlockPos> result = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (!result.add(current)) continue; // already visited

            MinecraftUtils.forEachNeighbor(current, (neighborPos) -> {
                if (!result.contains(neighborPos) && allowedPositions.contains(neighborPos)) {
                    queue.add(neighborPos);
                }
            });
        }

        return result;
    }

    private Set<Network> getNeighborNetworks(BlockPos pos, NetworkTypeRegistry.NetworkType<?> networkType) {
        Set<Network> networkSet = new HashSet<>();
        MinecraftUtils.forEachNeighbor(pos, (neighborPos) -> {
            Network network = getNetworkByBlockPos(neighborPos);
            if (network != null && network.getType() == networkType) {
                networkSet.add(network);
            }
        });
        return networkSet;
    }

    public Network getNetworkByBlockPos(BlockPos blockPos) {
        return nodePositionIndex.get(blockPos);
    }

    public NetworkNode getNodeByBlockPos(BlockPos blockPos) {
        Network network = getNetworkByBlockPos(blockPos);
        if (network == null) return null;
        return network.getNodeIndex().findByBlockPos(blockPos);
    }

    public ServerLevel getServerLevel() {
        return serverLevel;
    }

    @Deprecated
    public Collection<Network> getAllNetworks() {
        return networks.values().stream().toList();
    }

    private enum ChangeType { ADD, REMOVE }
    private record NetworkChange(ChangeType type, NetworkNode node, BlockPos blockPos) {
        static NetworkChange add(NetworkNode node) {
            return new NetworkChange(ChangeType.ADD, node, null);
        }
        static NetworkChange remove(BlockPos pos) {
            return new NetworkChange(ChangeType.REMOVE, null, pos);
        }
    }
}

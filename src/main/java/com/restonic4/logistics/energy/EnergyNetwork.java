package com.restonic4.logistics.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EnergyNetwork {
    /*
        DATA
     */

    private final UUID id;
    private final Set<BlockPos> memberPositions = new HashSet<>();
    private final Map<BlockPos, EnergyNode> loadedNodes = new HashMap<>();
    private final Queue<Map.Entry<BlockPos, EnergyNode>> pendingRegistrations = new ConcurrentLinkedQueue<>();
    private final Queue<BlockPos> pendingRemovals = new ConcurrentLinkedQueue<>();

    private long energyBuffer;
    private long maxBuffer;

    private long stableProductionPerTick;
    private long stableConsumptionPerTick;
    private long lastSimulatedTick;

    private volatile boolean needsCatchUp = false;
    private volatile long catchUpTargetTick = 0;

    /*
        CONSTRUCTORS
     */

    public EnergyNetwork(UUID id) {
        this.id = id;
    }

    public static EnergyNetwork create() {
        return new EnergyNetwork(UUID.randomUUID());
    }

    /*
        NODES MANAGEMENT
     */

    public void addPosition(BlockPos pos) {
        memberPositions.add(pos.immutable());
    }

    public void removePosition(BlockPos pos) {
        memberPositions.remove(pos);
        unregisterNode(pos);
    }

    /*public void registerLoadedNode(BlockPos pos, EnergyNode node) {
        if (memberPositions.contains(pos)) {
            loadedNodes.put(pos.immutable(), node);
            node.setEnergyNetwork(this);
        }
    }*/
    public void registerLoadedNode(BlockPos pos, EnergyNode node) {
        if (memberPositions.contains(pos)) {
            pendingRegistrations.add(Map.entry(pos.immutable(), node));
            System.out.println("Registering node for " + this.id);
        }
    }

    /*public void unregisterNode(BlockPos pos) {
        loadedNodes.remove(pos);
    }*/
    public void unregisterNode(BlockPos pos) {
        pendingRemovals.add(pos.immutable());
        System.out.println("Un-Registering node for " + this.id);
    }

    public void flagForCatchUp(long currentTick) {
        needsCatchUp = true;
        catchUpTargetTick = currentTick;
    }

    public boolean containsPosition(BlockPos pos) {
        return memberPositions.contains(pos);
    }

    public Set<BlockPos> getMemberPositions() {
        return Collections.unmodifiableSet(memberPositions);
    }

    public Collection<EnergyNode> getLoadedNodes() {
        return List.copyOf(loadedNodes.values());
    }

    public boolean isEmpty() {
        return memberPositions.isEmpty();
    }

    /*
        RATES
     */

    private boolean drainPendingChanges() {
        boolean changed = false;

        BlockPos pos;
        while ((pos = pendingRemovals.poll()) != null) {
            loadedNodes.remove(pos);
            changed = true;
        }

        Map.Entry<BlockPos, EnergyNode> entry;
        while ((entry = pendingRegistrations.poll()) != null) {
            loadedNodes.put(entry.getKey(), entry.getValue());
            entry.getValue().setEnergyNetwork(this);
            changed = true;
        }

        return changed; // true if rates need recalculating
    }

    public void recalculateRates() {
        long production = 0;
        long consumption = 0;
        long buffer = 0;

        for (EnergyNode node : loadedNodes.values()) {
            if (node instanceof EnergyProducer producer) {
                OfflineEnergyProfile profile = producer.getOfflineProducerProfile();
                if (profile.contributesToAggregate()) {
                    production += profile.getRatePerTick();
                }
            }
            if (node instanceof EnergyConsumer consumer) {
                OfflineEnergyProfile profile = consumer.getOfflineConsumerProfile();
                if (profile.contributesToAggregate()) {
                    consumption += profile.getRatePerTick();
                }
            }
            if (node instanceof EnergyStorage storage) {
                buffer += storage.getMaxStoredEnergy();
            }
        }

        this.stableProductionPerTick = production;
        this.stableConsumptionPerTick = consumption;
        this.maxBuffer = Math.max(buffer, 1); // never 0 to avoid division issues
    }

    private void catchUpSimulation(long currentTick) {
        drainPendingChanges();

        if (lastSimulatedTick == 0 || currentTick <= lastSimulatedTick) {
            lastSimulatedTick = currentTick;
            return;
        }

        long ticksElapsed = currentTick - lastSimulatedTick;

        long produced = stableProductionPerTick * ticksElapsed;
        for (EnergyNode node : loadedNodes.values()) {
            if (node instanceof EnergyStorage storage) {
                OfflineEnergyProfile profile = storage.getOfflineProducerProfile();
                produced += profile.computeOfflineDelta(node, ticksElapsed, Long.MAX_VALUE);
            } else if (node instanceof EnergyProducer producer) {
                OfflineEnergyProfile profile = producer.getOfflineProducerProfile();
                if (profile.getMode() == OfflineEnergyProfile.Mode.CUSTOM) {
                    produced += profile.computeOfflineDelta(node, ticksElapsed, Long.MAX_VALUE);
                }
            }
        }

        energyBuffer = Math.min(energyBuffer + produced, maxBuffer);

        long consumed = stableConsumptionPerTick * ticksElapsed;
        for (EnergyNode node : loadedNodes.values()) {
            if (node instanceof EnergyStorage storage) {
                OfflineEnergyProfile profile = storage.getOfflineConsumerProfile();
                consumed += profile.computeOfflineDelta(node, ticksElapsed, energyBuffer);
            } else if (node instanceof EnergyConsumer consumer) {
                OfflineEnergyProfile profile = consumer.getOfflineConsumerProfile();
                if (profile.getMode() == OfflineEnergyProfile.Mode.CUSTOM) {
                    consumed += profile.computeOfflineDelta(node, ticksElapsed, energyBuffer);
                }
            }
        }

        energyBuffer = Math.max(0, energyBuffer - consumed);
        lastSimulatedTick = currentTick;
    }

    public void mergeFrom(EnergyNetwork other) {
        memberPositions.addAll(other.memberPositions);

        other.drainPendingChanges();
        pendingRegistrations.addAll(other.loadedNodes.entrySet());

        energyBuffer = energyBuffer + other.energyBuffer;

        if (other.lastSimulatedTick > 0 && (lastSimulatedTick == 0 || other.lastSimulatedTick < lastSimulatedTick)) {
            lastSimulatedTick = other.lastSimulatedTick;
        }
    }

    public void seedBuffer(long amount) {
        energyBuffer = Math.max(0, amount);
    }

    public void clampBuffer() {
        energyBuffer = Math.max(0, Math.min(energyBuffer, maxBuffer));
    }

    /*
        TICKING
     */

    public void tick(long currentTick) {
        boolean changed = drainPendingChanges();

        if (needsCatchUp) {
            catchUpSimulation(catchUpTargetTick);
            needsCatchUp = false;
            changed = true;
        }

        if (changed) {
            recalculateRates();
        }

        if (this.getLoadedNodes().isEmpty()) {
            return;
        }

        List<EnergyNode> activeNodes = new ArrayList<>();
        for (EnergyNode node : loadedNodes.values()) {
            if (node.isChunkTicking()) {
                activeNodes.add(node);
            }
        }

        if (activeNodes.isEmpty()) {
            return;
        }

        long available = energyBuffer;

        // Step 1: collect from regular producers
        for (EnergyNode node : activeNodes) {
            if (node instanceof EnergyStorage) continue;
            if (node instanceof EnergyProducer producer) {
                available += producer.produceEnergy(Long.MAX_VALUE);
            }
        }

        // Step 2: discharge batteries
        for (EnergyNode node : activeNodes) {
            if (node instanceof EnergyStorage storage && storage.canDischarge()) {
                available += storage.produceEnergy(Long.MAX_VALUE);
            }
        }

        // Step 3: distribute to regular consumers proportionally
        long totalDemand = 0;
        List<EnergyConsumer> activeConsumers = new ArrayList<>();
        for (EnergyNode node : activeNodes) {
            if (node instanceof EnergyStorage) continue;
            if (node instanceof EnergyConsumer consumer && consumer.needsEnergy()) {
                totalDemand += consumer.getMaxConsumptionPerTick();
                activeConsumers.add(consumer);
            }
        }

        for (EnergyConsumer consumer : activeConsumers) {
            if (available <= 0) break;
            long desired = consumer.getMaxConsumptionPerTick();
            long offer = (totalDemand > 0 && available < totalDemand)
                    ? (long) ((double) desired / totalDemand * available)
                    : desired;
            offer = Math.min(offer, available);
            available -= consumer.consumeEnergy(offer);
        }

        // Step 4: charge batteries
        for (EnergyNode node : activeNodes) {
            if (node instanceof EnergyStorage storage && storage.canCharge()) {
                if (available <= 0) break;
                available -= storage.consumeEnergy(available);
            }
        }

        // Step 5: store remaining in network buffer
        energyBuffer = Math.max(0, Math.min(available, maxBuffer));
        lastSimulatedTick = currentTick;
    }

    /*
        SAVING AND LOADING
     */

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putUUID("id", id);
        tag.putLong("energyBuffer", energyBuffer);
        tag.putLong("maxBuffer", maxBuffer);
        tag.putLong("stableProductionPerTick", stableProductionPerTick);
        tag.putLong("stableConsumptionPerTick", stableConsumptionPerTick);
        tag.putLong("lastSimulatedTick", lastSimulatedTick);

        ListTag positions = new ListTag();
        for (BlockPos pos : memberPositions) {
            positions.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put("members", positions);

        return tag;
    }

    public static EnergyNetwork load(CompoundTag tag) {
        UUID id = tag.getUUID("id");
        EnergyNetwork network = new EnergyNetwork(id);

        network.energyBuffer = tag.getLong("energyBuffer");
        network.maxBuffer = tag.getLong("maxBuffer");
        network.stableProductionPerTick = tag.getLong("stableProductionPerTick");
        network.stableConsumptionPerTick = tag.getLong("stableConsumptionPerTick");
        network.lastSimulatedTick = tag.getLong("lastSimulatedTick");

        ListTag positions = tag.getList("members", Tag.TAG_LONG);
        for (Tag t : positions) {
            network.memberPositions.add(BlockPos.of(((LongTag) t).getAsLong()));
        }

        return network;
    }

    /*
        GETTERS
     */

    public UUID getId() { return id; }
    public long getEnergyBuffer() { return energyBuffer; }
    public long getMaxBuffer() { return maxBuffer; }
    public long getStableProductionPerTick() { return stableProductionPerTick; }
    public long getStableConsumptionPerTick() { return stableConsumptionPerTick; }
    public long getLastSimulatedTick() { return lastSimulatedTick; }
    public int getMemberCount() { return memberPositions.size(); }
}
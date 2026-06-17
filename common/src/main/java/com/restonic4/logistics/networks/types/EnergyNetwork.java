package com.restonic4.logistics.networks.types;

import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.blocks.audio_station.AudioStationNode;
import com.restonic4.logistics.blocks.cable.CableNode;
import com.restonic4.logistics.blocks.network_connector.NetworkConnectorNode;
import com.restonic4.logistics.blocks.network_switch.NetworkSwitchNode;
import com.restonic4.logistics.blocks.protector.ProtectorNode;
import com.restonic4.logistics.blocks.redstone_reader.RedstoneReaderNode;
import com.restonic4.logistics.blocks.protector.data_types.ProtectionZone;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.flags.NetworkFlag;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.registry.NetworkTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EnergyNetwork extends Network {
    public static final int PIPE_EXTRA_BUFFER = 1;

    private long networkCableBuffer = 0;

    private long cacheStoredNodeEnergyBuffer = 0;
    private long cacheTotalNodeEnergyBuffer = 0;
    private long cacheTotalCableEnergyBuffer = 0;

    private long lastTotalUpdate = 0;
    private long lastStoredUpdate = 0;

    // Per-tick energy metering. Accumulators fill during the current tick (as nodes
    // request/report energy) and are snapshotted into the "last tick" values at the
    // start of the next tick, so the getters always expose a fully completed tick.
    private long productionAccumulator = 0;
    private long optimisticProductionAccumulator = 0;
    private long consumptionAccumulator = 0;
    private long lastTickProduction = 0;
    private long lastTickOptimisticProduction = 0;
    private long lastTickConsumption = 0;

    public EnergyNetwork(NetworkTypeRegistry.NetworkType<?> type, ServerLevel serverLevel) {
        super(type, serverLevel);
    }

    public void tick() {
        lastTickProduction = productionAccumulator;
        lastTickOptimisticProduction = optimisticProductionAccumulator;
        lastTickConsumption = consumptionAccumulator;
        productionAccumulator = 0;
        optimisticProductionAccumulator = 0;
        consumptionAccumulator = 0;

        super.tick();
        recalculateCaches();
    }

    @Override
    public void mergeDataFrom(Network other) {
        if (other instanceof EnergyNetwork energyOther) {
            this.networkCableBuffer += energyOther.networkCableBuffer;
        }
    }

    @Override
    public void onSplit(Collection<Network> children) {
        if (children.isEmpty() || networkCableBuffer <= 0) return;

        int totalNodes = this.getNodeIndex().size();
        long remainingEnergy = networkCableBuffer;

        for (Network child : children) {
            if (child instanceof EnergyNetwork energyChild) {
                double share = (double) energyChild.getNodeIndex().size() / totalNodes;
                long amount = (long) (this.networkCableBuffer * share);

                energyChild.networkCableBuffer = amount;
                remainingEnergy -= amount;
            }
        }

        if (remainingEnergy > 0 && !children.isEmpty()) {
            Network first = children.iterator().next();
            if (first instanceof EnergyNetwork eFirst) eFirst.networkCableBuffer += remainingEnergy;
        }
    }

    public long requestEnergyConsumption(long desired) {
        long extracted = 0;

        // Drain from cables
        if (networkCableBuffer > 0) {
            long fromBuffer = Math.min(desired, networkCableBuffer);
            networkCableBuffer -= fromBuffer;
            extracted += fromBuffer;
        }

        // Drain from nodes
        if (extracted < desired) {
            for (NetworkNode node : this.getNodeIndex().getAllNodes()) {
                if (node instanceof EnergyNode energyNode) {
                    long toGet = desired - extracted;
                    extracted += energyNode.extractEnergy(toGet, false);
                    if (extracted >= desired) break;
                }
            }
        }

        consumptionAccumulator += extracted;
        setDirty();
        return extracted;
    }

    public long reportEnergyProduction(long produced) {
        long remaining = produced;

        // Fill cable buffer
        long bufferSpace = getTotalCableEnergyBuffer() - networkCableBuffer;
        if (bufferSpace > 0) {
            long toBuffer = Math.min(remaining, bufferSpace);
            networkCableBuffer += toBuffer;
            remaining -= toBuffer;
        }

        // Fill nodes
        if (remaining > 0) {
            for (NetworkNode node : this.getNodeIndex().getAllNodes()) {
                if (node instanceof EnergyNode energyNode) {
                    remaining = energyNode.receiveEnergy(remaining, false);
                    if (remaining <= 0) break;
                }
            }
        }

        productionAccumulator += produced - remaining;
        optimisticProductionAccumulator += produced;
        setDirty();
        return remaining;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        tag.putLong("networkCableBuffer", networkCableBuffer);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        this.networkCableBuffer = tag.getLong("networkCableBuffer");
    }

    public void recalculateCaches() {
        if (isDirty(NetworkFlag.STORAGE_CHANGED)) {
            cacheStoredNodeEnergyBuffer = getNodeIndex().getAllNodes().stream().mapToLong(n -> n instanceof EnergyNode e ? e.getStoredEnergy() : 0).sum();
            clearFlag(NetworkFlag.STORAGE_CHANGED);
            lastStoredUpdate = getServerLevel().getGameTime();
        }

        if (isDirty(NetworkFlag.MAX_STORAGE_CHANGED)) {
            cacheTotalNodeEnergyBuffer = getNodeIndex().getAllNodes().stream().mapToLong(n -> n instanceof EnergyNode e ? e.getMaxStorage() : 0).sum();
            cacheTotalCableEnergyBuffer = getNodeIndex().getAllNodes().stream().filter(n -> n instanceof CableNode).count() * PIPE_EXTRA_BUFFER;
            clearFlag(NetworkFlag.MAX_STORAGE_CHANGED);
            lastTotalUpdate = getServerLevel().getGameTime();
        }
    }

    public static String formatEnergy(long eu) {
        if (eu >= 1_000_000) return String.format("%.1fMEU", eu / 1_000_000.0);
        if (eu >= 1_000) return String.format("%.1fkEU", eu / 1_000.0);
        return eu + " EU";
    }

    public static String formatTicks(long ticks) {
        double seconds = ticks / 20.0;
        if (seconds >= 3600) return String.format("%.1fh", seconds / 3600.0);
        if (seconds >= 60) return String.format("%.1fm", seconds / 60.0);
        return String.format("%.1fs", seconds);
    }

    public long getStoredEnergyBuffer() { return cacheStoredNodeEnergyBuffer + getStoredCableEnergyBuffer(); }
    public long getTotalEnergyBuffer() { return cacheTotalNodeEnergyBuffer + getTotalCableEnergyBuffer(); }

    /**
     * Sentinel returned by the prediction getters when the network is not heading toward the
     * given state at the current rate (e.g. asking for ticks-until-empty while it is charging).
     */
    public static final long NEVER = -1;

    /** Energy added to the network during the last fully completed tick (EU/tick). */
    public long getLastTickProduction() { return lastTickProduction; }

    public long getLastTickOptimisticProduction() { return lastTickOptimisticProduction; }

    /** Energy drained from the network during the last fully completed tick (EU/tick). */
    public long getLastTickConsumption() { return lastTickConsumption; }

    /** Net energy change during the last tick. Positive: charging, negative: draining. */
    public long getLastTickNetEnergy() { return lastTickProduction - lastTickConsumption; }

    /**
     * Estimated ticks until the network's stored energy runs out, extrapolating from the last
     * tick's net flow. Returns {@link #NEVER} when the network is stable or charging.
     */
    public long getTicksUntilEmpty() {
        long net = getLastTickNetEnergy();
        if (net >= 0) return NEVER;
        return getStoredEnergyBuffer() / -net;
    }

    /**
     * Estimated ticks until the network reaches full capacity, extrapolating from the last
     * tick's net flow. Returns {@link #NEVER} when the network is stable or draining.
     */
    public long getTicksUntilFull() {
        long net = getLastTickNetEnergy();
        if (net <= 0) return NEVER;
        long space = getTotalEnergyBuffer() - getStoredEnergyBuffer();
        if (space <= 0) return 0;
        return space / net;
    }

    /** Convenience: {@link #getTicksUntilEmpty()} expressed in seconds, or {@link #NEVER}. */
    public double getSecondsUntilEmpty() {
        long ticks = getTicksUntilEmpty();
        return ticks == NEVER ? NEVER : ticks / 20.0;
    }

    /** Convenience: {@link #getTicksUntilFull()} expressed in seconds, or {@link #NEVER}. */
    public double getSecondsUntilFull() {
        long ticks = getTicksUntilFull();
        return ticks == NEVER ? NEVER : ticks / 20.0;
    }

    public long getStoredCableEnergyBuffer() { return networkCableBuffer; }
    public long getTotalCableEnergyBuffer() { return cacheTotalCableEnergyBuffer; }
    public void setStoredCableEnergyBuffer(long buffer) { this.networkCableBuffer = buffer; }

    public Set<NetworkConnectorNode> getNetworkConnectors() {
        return getNodeIndex().getAllNodes().stream()
                .filter(node -> node instanceof NetworkConnectorNode)
                .map(node -> (NetworkConnectorNode) node)
                .collect(Collectors.toSet());
    }

    public Set<ProtectorNode> getProtectors() {
        return getNodeIndex().getAllNodes().stream()
                .filter(node -> node instanceof ProtectorNode)
                .map(node -> (ProtectorNode) node)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildDebugScannerTooltip(builder, isSneaking);

        builder.spacer();
        builder.timeSinceTick("Last total buffer update:", lastTotalUpdate, getServerLevel().getGameTime(), ChatFormatting.YELLOW);
        builder.timeSinceTick("Last stored buffer update:", lastStoredUpdate, getServerLevel().getGameTime(), ChatFormatting.YELLOW);

        builder.spacer();
        builder.keyValue("Production:", formatEnergy(getLastTickProduction()) + "/t", ChatFormatting.YELLOW);
        builder.keyValue("Consumption:", formatEnergy(getLastTickConsumption()) + "/t", ChatFormatting.YELLOW);
        builder.keyValue("Net:", formatEnergy(getLastTickNetEnergy()) + "/t", ChatFormatting.YELLOW);

        long ticksUntilEmpty = getTicksUntilEmpty();
        long ticksUntilFull = getTicksUntilFull();
        if (ticksUntilEmpty != NEVER) {
            builder.keyValue("Time until empty:", formatTicks(ticksUntilEmpty), ChatFormatting.YELLOW);
        } else if (ticksUntilFull != NEVER) {
            builder.keyValue("Time until full:", formatTicks(ticksUntilFull), ChatFormatting.YELLOW);
        }

        return true;
    }

    public boolean hasAudioStations() {
        for (NetworkNode networkNode : getNodeIndex().getAllNodes()) {
            if (networkNode instanceof AudioStationNode) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAccessors() {
        for (NetworkNode networkNode : getNodeIndex().getAllNodes()) {
            if (networkNode instanceof NetworkConnectorNode connectorNode) {
                return connectorNode.hasAccessors();
            }
        }
        return false;
    }

    public boolean hasProtectors() {
        for (NetworkNode networkNode : getNodeIndex().getAllNodes()) {
            if (networkNode instanceof ProtectorNode) {
                return true;
            }
        }
        return false;
    }

    public List<AudioStationNode> getAudioStations() {
        List<AudioStationNode> audioNodes = new ArrayList<>();
        for (NetworkNode networkNode : getNodeIndex().getAllNodes()) {
            if (networkNode instanceof AudioStationNode audioNode) {
                audioNodes.add(audioNode);
            }
        }

        return audioNodes;
    }

    public List<AccessorNode> getAccessors() {
        List<AccessorNode> nodes = new ArrayList<>();
        for (NetworkNode networkNode : getNodeIndex().getAllNodes()) {
            if (networkNode instanceof NetworkConnectorNode connectorNode) {
                List<AccessorNode> accessors = connectorNode.getAccessors();
                if (accessors != null) nodes.addAll(accessors);
            }
        }

        return nodes;
    }

    public List<RedstoneReaderNode> getRedstoneReaders() {
        List<RedstoneReaderNode> nodes = new ArrayList<>();
        for (NetworkNode networkNode : getNodeIndex().getAllNodes()) {
            if (networkNode instanceof RedstoneReaderNode readerNode) {
                nodes.add(readerNode);
            }
        }

        return nodes;
    }

    public List<NetworkSwitchNode> getNetworkSwitches() {
        List<NetworkSwitchNode> nodes = new ArrayList<>();
        for (NetworkNode networkNode : getNodeIndex().getAllNodes()) {
            if (networkNode instanceof NetworkSwitchNode switchNode) {
                nodes.add(switchNode);
            }
        }

        return nodes;
    }

    public List<ProtectionZone> getProtectionZones() {
        List<ProtectionZone> zones = new ArrayList<>();
        for (ProtectorNode protectorNode : this.getProtectors()) {
            zones.add(new ProtectionZone(
                    protectorNode.getUUID(),
                    protectorNode.getBlockPos(),
                    protectorNode.getRadius(),
                    protectorNode.isCreative(),
                    protectorNode.getSafeName(),
                    protectorNode.getRoles(),
                    protectorNode.isPowered()
            ));
        }
        return zones;
    }
}

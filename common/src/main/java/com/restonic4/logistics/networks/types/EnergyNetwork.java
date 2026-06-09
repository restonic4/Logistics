package com.restonic4.logistics.networks.types;

import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.blocks.audio_station.AudioStationNode;
import com.restonic4.logistics.blocks.cable.CableNode;
import com.restonic4.logistics.blocks.computer.ComputerSyncPacket;
import com.restonic4.logistics.blocks.network_connector.NetworkConnectorNode;
import com.restonic4.logistics.blocks.protector.ProtectorNode;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EnergyNetwork extends Network {
    public static final int PIPE_EXTRA_BUFFER = 1;

    private long networkCableBuffer = 0;

    private long cacheStoredNodeEnergyBuffer = 0;
    private long cacheTotalNodeEnergyBuffer = 0;
    private long cacheTotalCableEnergyBuffer = 0;

    private long lastTotalUpdate = 0;
    private long lastStoredUpdate = 0;

    public EnergyNetwork(NetworkTypeRegistry.NetworkType<?> type, ServerLevel serverLevel) {
        super(type, serverLevel);
    }

    public void tick() {
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

    public long getStoredEnergyBuffer() { return cacheStoredNodeEnergyBuffer + getStoredCableEnergyBuffer(); }
    public long getTotalEnergyBuffer() { return cacheTotalNodeEnergyBuffer + getTotalCableEnergyBuffer(); }

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

    public List<AudioStationNode.AudioStationData> getAudioStationsData() {
        List<AudioStationNode> audioNodes = new ArrayList<>();
        for (NetworkNode networkNode : getNodeIndex().getAllNodes()) {
            if (networkNode instanceof AudioStationNode audioNode) {
                audioNodes.add(audioNode);
            }
        }

        if (!audioNodes.isEmpty()) {
            List<AudioStationNode.AudioStationData> audioData = new ArrayList<>();
            for (AudioStationNode audioStationNode : audioNodes) {
                audioData.add(new AudioStationNode.AudioStationData(
                        audioStationNode.getBlockPos(),
                        audioStationNode.getAudioPath(),
                        audioStationNode.getVolume(),
                        audioStationNode.getPitch(),
                        audioStationNode.getRadius(),
                        audioStationNode.isLooping(),
                        audioStationNode.isRedstoneMode()
                ));
            }
            return audioData;
        }

        return null;
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
}

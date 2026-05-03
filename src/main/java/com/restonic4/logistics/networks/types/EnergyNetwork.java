package com.restonic4.logistics.networks.types;

import com.restonic4.logistics.blocks.pipe.PipeNode;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.registry.NetworkTypeRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;

public class EnergyNetwork extends Network {
    public static final int PIPE_EXTRA_BUFFER = 1;

    private long networkCableBuffer = 0;

    private long cacheStoredNodeEnergyBuffer = 0;
    private long cacheTotalNodeEnergyBuffer = 0;
    private long cacheTotalCableEnergyBuffer = 0;

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

    // TODO: optimize this nonsense lol
    public void recalculateCaches() {
        cacheStoredNodeEnergyBuffer = this.getNodeIndex().getAllNodes().stream().mapToLong(node -> {
            if (node instanceof EnergyNode energyNode) {
                return energyNode.getStoredEnergy();
            }
            return 0;
        }).sum();
        cacheTotalNodeEnergyBuffer = this.getNodeIndex().getAllNodes().stream().mapToLong(node -> {
            if (node instanceof EnergyNode energyNode) {
                return energyNode.getMaxStorage();
            }
            return 0;
        }).sum();
        cacheTotalCableEnergyBuffer = this.getNodeIndex().getAllNodes().stream().filter(n -> n instanceof PipeNode).count() * PIPE_EXTRA_BUFFER;
    }

    public long getStoredEnergyBuffer() { return cacheStoredNodeEnergyBuffer + getStoredCableEnergyBuffer(); }
    public long getTotalEnergyBuffer() { return cacheTotalNodeEnergyBuffer + getTotalCableEnergyBuffer(); }

    public long getStoredCableEnergyBuffer() { return networkCableBuffer; }
    public long getTotalCableEnergyBuffer() { return cacheTotalCableEnergyBuffer; }
    public void setStoredCableEnergyBuffer(long buffer) { this.networkCableBuffer = buffer; }
}

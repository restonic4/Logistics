package com.restonic4.logistics.networks.energy;

import com.restonic4.logistics.blocks.pipe.PipeNode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class Network {
    public static final int PIPE_EXTRA_BUFFER = 1;

    private final UUID uuid;
    private final NodeIndex nodeIndex;
    private final ServerLevel serverLevel;

    private long networkCableBuffer = 0;

    private long cacheStoredNodeEnergyBuffer = 0;
    private long cacheTotalNodeEnergyBuffer = 0;
    private long cacheTotalCableEnergyBuffer = 0;
    private boolean isDirty = false;

    private Network(UUID uuid, ServerLevel serverLevel) {
        this.uuid = uuid;
        this.serverLevel = serverLevel;
        this.nodeIndex = new NodeIndex(this);
    }

    public static Network create(ServerLevel serverLevel) {
        return new Network(UUID.randomUUID(), serverLevel);
    }

    public void tick() {
        nodeIndex.getAllNodes().forEach(NetworkNode::tick);
        recalculateCaches();
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
            for (NetworkNode node : nodeIndex.getAllNodes()) {
                long toGet = desired - extracted;
                extracted += node.extractEnergy(toGet, false);
                if (extracted >= desired) break;
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
            for (NetworkNode node : nodeIndex.getAllNodes()) {
                remaining = node.receiveEnergy(remaining, false);
                if (remaining <= 0) break;
            }
        }

        setDirty();
        return remaining;
    }

    public NodeIndex getNodeIndex() {
        return nodeIndex;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putUUID("uuid", uuid);
        tag.putLong("networkCableBuffer", networkCableBuffer);
        ListTag nodes = new ListTag();
        for (NetworkNode node : nodeIndex.getAllNodes()) {
            nodes.add(node.save());
        }
        tag.put("nodes", nodes);

        return tag;
    }

    public static Network load(CompoundTag tag, ServerLevel serverLevel) {
        UUID id = tag.getUUID("uuid");
        Network network = new Network(id, serverLevel);

        network.networkCableBuffer = tag.getLong("networkCableBuffer");
        ListTag nodesList = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < nodesList.size(); i++) {
            network.nodeIndex.registerFromCompoundTag(nodesList.getCompound(i));
        }

        return network;
    }

    // TODO: optimize this nonsense lol
    public void recalculateCaches() {
        cacheStoredNodeEnergyBuffer = nodeIndex.getAllNodes().stream().mapToLong(NetworkNode::getStoredEnergy).sum();
        cacheTotalNodeEnergyBuffer = nodeIndex.getAllNodes().stream().mapToLong(NetworkNode::getMaxStorage).sum();
        cacheTotalCableEnergyBuffer = nodeIndex.getAllNodes().stream().filter(n -> n instanceof PipeNode).count() * PIPE_EXTRA_BUFFER;
    }

    public UUID getUUID() { return uuid; }
    public ServerLevel getServerLevel() { return serverLevel; }

    public long getStoredEnergyBuffer() { return cacheStoredNodeEnergyBuffer + getStoredCableEnergyBuffer(); }
    public long getTotalEnergyBuffer() { return cacheTotalNodeEnergyBuffer + getTotalCableEnergyBuffer(); }

    public long getStoredCableEnergyBuffer() { return networkCableBuffer; }
    public long getTotalCableEnergyBuffer() { return cacheTotalCableEnergyBuffer; }
    public void setStoredCableEnergyBuffer(long buffer) { this.networkCableBuffer = buffer; }

    public void setDirty() { this.isDirty = true; }
    public void cleanDirtyFlag() { this.isDirty = false; }
    public boolean isDirty() { return this.isDirty; }
}

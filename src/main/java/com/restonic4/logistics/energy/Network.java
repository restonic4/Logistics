package com.restonic4.logistics.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class Network {
    private final UUID uuid;
    private final NodeIndex nodeIndex;
    private final ServerLevel serverLevel;

    private long cacheStoredEnergyBuffer = 0;
    private long cacheTotalEnergyBuffer = 0;
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

        cacheStoredEnergyBuffer = nodeIndex.getAllNodes().stream().mapToLong(NetworkNode::getStoredEnergy).sum();
        cacheTotalEnergyBuffer = nodeIndex.getAllNodes().stream().mapToLong(NetworkNode::getMaxStorage).sum();
    }

    public long requestEnergyConsumption(long desired) {
        long extracted = 0;
        for (NetworkNode node : nodeIndex.getAllNodes()) {
            long toGet = desired - extracted;
            extracted += node.extractEnergy(toGet, false);
            if (extracted >= desired) break;
        }
        setDirty();
        return extracted;
    }

    public long reportEnergyProduction(long produced) {
        long remaining = produced;
        for (NetworkNode node : nodeIndex.getAllNodes()) {
            remaining = node.receiveEnergy(remaining, false);
            if (remaining <= 0) break;
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

        ListTag nodesList = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < nodesList.size(); i++) {
            network.nodeIndex.registerFromCompoundTag(nodesList.getCompound(i));
        }

        return network;
    }

    public UUID getUUID() {
        return uuid;
    }

    public ServerLevel getServerLevel() {
        return serverLevel;
    }

    public long getStoredEnergyBuffer() {
        return cacheStoredEnergyBuffer;
    }

    public Object getTotalEnergyBuffer() {
        return cacheTotalEnergyBuffer;
    }

    public void setDirty() {
        this.isDirty = true;
    }

    public void cleanDirtyFlag() {
        this.isDirty = false;
    }

    public boolean isDirty() {
        return this.isDirty;
    }
}

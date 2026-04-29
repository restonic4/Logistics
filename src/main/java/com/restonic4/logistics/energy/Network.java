package com.restonic4.logistics.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;

public class Network {
    private final UUID uuid;
    private final NodeIndex nodeIndex;

    private long cacheStoredEnergyBuffer = 0;
    private long cacheTotalEnergyBuffer = 0;

    private Network(UUID uuid) {
        this.uuid = uuid;
        this.nodeIndex = new NodeIndex(this);
    }

    public static Network create() {
        return new Network(UUID.randomUUID());
    }

    public void tick() {
        for (NetworkNode node : nodeIndex.getAllNodes()) {
            node.tick();
        }

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
        return extracted;
    }

    public long reportEnergyProduction(long produced) {
        long remaining = produced;
        for (NetworkNode node : nodeIndex.getAllNodes()) {
            remaining = node.receiveEnergy(remaining, false);
            if (remaining <= 0) break;
        }
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

    public static Network load(CompoundTag tag) {
        UUID id = tag.getUUID("uuid");
        Network network = new Network(id);

        ListTag nodesList = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < nodesList.size(); i++) {
            network.nodeIndex.registerFromCompoundTag(nodesList.getCompound(i));
        }

        return network;
    }

    public UUID getUUID() {
        return uuid;
    }

    public long getStoredEnergyBuffer() {
        return cacheStoredEnergyBuffer;
    }

    public Object getTotalEnergyBuffer() {
        return cacheTotalEnergyBuffer;
    }
}

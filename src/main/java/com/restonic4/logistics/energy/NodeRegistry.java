package com.restonic4.logistics.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;


public final class NodeRegistry {
    private final Network network;

    private final Map<UUID, NetworkNode> byUUID;
    private final Map<BlockPos, NetworkNode> byBlockPos;

    private final Queue<NodeChange> pendingChanges = new ConcurrentLinkedQueue<>();

    public NodeRegistry(Network network) {
        this.network = network;
        this.byUUID = new HashMap<>();
        this.byBlockPos = new HashMap<>();
    }

    public void applyChanges() {
        while (!pendingChanges.isEmpty()) {
            NodeChange change = pendingChanges.poll();
            switch (change.type()) {
                case ADD -> internalRegister(change.node());
                case REMOVE -> internalUnregister(change.node());
                case MOVE -> internalReIndex(change.node(), change.oldPos(), change.newPos());
            }
        }
    }

    public void internalRegister(NetworkNode node) {
        node.setNetwork(network);
        byUUID.put(node.getUUID(), node);
        byBlockPos.put(node.getBlockPos(), node);
    }

    public void internalUnregister(NetworkNode node) {
        node.setNetwork(null);
        byUUID.remove(node.getUUID());
        byBlockPos.remove(node.getBlockPos());
    }

    public void internalReIndex(NetworkNode node, BlockPos oldPos, BlockPos newPos) {
        if (oldPos != null) byBlockPos.remove(oldPos);
        if (newPos != null) byBlockPos.put(newPos, node);
    }

    public void registerFromCompoundTag(CompoundTag compoundTag) {
        internalRegister(NetworkNode.createFromTag(compoundTag));
    }

    public NetworkNode findByUUID(UUID uuid) { return byUUID.get(uuid); }
    public NetworkNode findByBlockPos(BlockPos blockPos) { return byBlockPos.get(blockPos); }

    public Collection<NetworkNode> getAllNodes() {
        return byUUID.values();
    }

    private enum ChangeType { ADD, REMOVE, MOVE }
    private record NodeChange(ChangeType type, NetworkNode node, BlockPos oldPos, BlockPos newPos) {}
}

package com.restonic4.logistics.networks;

import com.restonic4.logistics.networks.flags.NetworkFlag;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.*;

public final class NodeIndex {
    private final Network network;

    private final Map<UUID, NetworkNode> byUUID;
    private final Map<BlockPos, NetworkNode> byBlockPos;

    public NodeIndex(Network network) {
        this.network = network;
        this.byUUID = new HashMap<>();
        this.byBlockPos = new HashMap<>();
    }

    public void register(NetworkNode node) {
        node.setNetwork(network);
        byUUID.put(node.getUUID(), node);
        byBlockPos.put(node.getBlockPos(), node);
        network.markDirty(NetworkFlag.NODE_ADDED);
        network.markDirty(NetworkFlag.MAX_STORAGE_CHANGED); // Meh
    }

    public void unregister(NetworkNode node) {
        node.setNetwork(null);
        byUUID.remove(node.getUUID());
        byBlockPos.remove(node.getBlockPos());
        network.markDirty(NetworkFlag.NODE_REMOVED);
        network.markDirty(NetworkFlag.MAX_STORAGE_CHANGED); // Meh
    }

    public void registerFromCompoundTag(CompoundTag compoundTag) {
        register(NetworkNode.createFromTag(compoundTag));
    }

    public NetworkNode findByUUID(UUID uuid) { return byUUID.get(uuid); }
    public NetworkNode findByBlockPos(BlockPos blockPos) { return byBlockPos.get(blockPos); }

    public Collection<NetworkNode> getAllNodes() {
        return byUUID.values();
    }
    public Collection<BlockPos> getAllNodesPositions() {
        return byBlockPos.keySet();
    }
    public Set<Long> getAllNodePositionsAsLongs() {
        Set<Long> longs = new HashSet<>();
        byBlockPos.keySet().forEach(blockPos -> longs.add(blockPos.asLong()));
        return longs;
    }
    public int size() {
        return byUUID.size();
    }
}

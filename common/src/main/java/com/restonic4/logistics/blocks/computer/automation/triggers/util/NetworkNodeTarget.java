package com.restonic4.logistics.blocks.computer.automation.triggers.util;

import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A reusable "which node(s)?" selection for triggers/actions: either every candidate node of a kind
 * on the computer's network, or one specific node by UUID. Resolution is done against a pre-filtered
 * list supplied by the caller (e.g. {@code ctx.getRedstoneReaders()}), so this stays type-agnostic.
 * Embeds its own serialization so owning triggers/actions just delegate to it.
 *
 * @param <T> the node kind this target selects
 */
public final class NetworkNodeTarget<T extends NetworkNode> {
    private static final String TAG_ALL = "allNodes";
    private static final String TAG_NODE = "nodeId";

    private boolean all = true;
    private UUID nodeId = null;

    /** Whether this targets every candidate node on the network. */
    public boolean isAll() { return all; }

    /** The targeted node's UUID; only meaningful when {@link #isAll()} is false. */
    public UUID getNodeId() { return nodeId; }

    public void setAll() {
        this.all = true;
        this.nodeId = null;
    }

    public void setNode(UUID nodeId) {
        this.all = false;
        this.nodeId = nodeId;
    }

    /**
     * Resolves this target against a candidate list.
     *
     * @return all candidates when targeting "all"; the single matching node (or empty) otherwise
     */
    public List<T> resolve(List<T> candidates) {
        if (all) return candidates;
        if (nodeId == null) return List.of();
        List<T> result = new ArrayList<>(1);
        for (T candidate : candidates) {
            if (candidate.getUUID().equals(nodeId)) {
                result.add(candidate);
                break;
            }
        }
        return result;
    }

    public void save(CompoundTag tag) {
        tag.putBoolean(TAG_ALL, all);
        if (nodeId != null) {
            tag.putUUID(TAG_NODE, nodeId);
        }
    }

    public void load(CompoundTag tag) {
        this.all = tag.getBoolean(TAG_ALL);
        this.nodeId = tag.hasUUID(TAG_NODE) ? tag.getUUID(TAG_NODE) : null;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(all);
        buf.writeBoolean(nodeId != null);
        if (nodeId != null) {
            buf.writeUUID(nodeId);
        }
    }

    public void read(FriendlyByteBuf buf) {
        this.all = buf.readBoolean();
        this.nodeId = buf.readBoolean() ? buf.readUUID() : null;
    }
}

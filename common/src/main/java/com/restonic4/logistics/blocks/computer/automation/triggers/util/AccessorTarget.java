package com.restonic4.logistics.blocks.computer.automation.triggers.util;

import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A reusable "which accessor(s)?" selection shared by item triggers and actions: either
 * every accessor bridged to the computer's network, or one specific accessor by node UUID.
 * What the "any" state means depends on the owner — a trigger reads it as "all accessors
 * combined", a send action as "auto-pick a source". Embeds its own serialization so owning
 * triggers/actions just delegate to it.
 */
public final class AccessorTarget {
    private static final String TAG_ANY = "anyAccessor";
    private static final String TAG_ACCESSOR = "accessorId";

    private boolean any = true;
    private UUID accessorId = null;

    /** Whether this targets every accessor (trigger scope) / no particular one (auto source). */
    public boolean isAny() { return any; }

    /** The targeted accessor's node UUID; only meaningful when {@link #isAny()} is false. */
    public UUID getAccessorId() { return accessorId; }

    public void setAny() {
        this.any = true;
        this.accessorId = null;
    }

    public void setAccessor(UUID accessorId) {
        this.any = false;
        this.accessorId = accessorId;
    }

    /**
     * Resolves this target against the tick snapshot.
     *
     * @return the targeted accessors; empty if a specific accessor no longer exists
     */
    public List<AccessorNode> resolve(TriggerContext ctx) {
        if (any) return ctx.getAccessors();
        if (accessorId == null) return Collections.emptyList();
        AccessorNode accessor = ctx.findAccessor(accessorId);
        return accessor != null ? List.of(accessor) : Collections.emptyList();
    }

    public void save(CompoundTag tag) {
        tag.putBoolean(TAG_ANY, any);
        if (accessorId != null) {
            tag.putUUID(TAG_ACCESSOR, accessorId);
        }
    }

    public void load(CompoundTag tag) {
        this.any = tag.getBoolean(TAG_ANY);
        this.accessorId = tag.hasUUID(TAG_ACCESSOR) ? tag.getUUID(TAG_ACCESSOR) : null;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(any);
        buf.writeBoolean(accessorId != null);
        if (accessorId != null) {
            buf.writeUUID(accessorId);
        }
    }

    public void read(FriendlyByteBuf buf) {
        this.any = buf.readBoolean();
        this.accessorId = buf.readBoolean() ? buf.readUUID() : null;
    }
}

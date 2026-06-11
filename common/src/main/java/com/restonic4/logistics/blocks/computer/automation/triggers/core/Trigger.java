package com.restonic4.logistics.blocks.computer.automation.triggers.core;

import com.restonic4.logistics.blocks.computer.automation.triggers.registry.TriggerRegistry;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.TriggerType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A user-configured condition that is evaluated every game tick by the {@link TriggerManager}.
 * When the condition fires (according to its {@link ExecutionMode}), the trigger's ordered
 * {@link TriggerAction} list is spawned as a new {@link ActionSequenceTracker} and executed
 * sequentially over the following ticks without ever blocking the tick loop.
 * <p>
 * Subclasses implement {@link #evaluate} plus the {@code saveExtra}/{@code loadExtra} and
 * {@code writeExtraSyncData}/{@code readExtraSyncData} hooks for their own configuration.
 */
public abstract class Trigger {
    private static final String TAG_TYPE = "type";
    private static final String TAG_INSTANCE_ID = "instanceId";
    private static final String TAG_MODE = "mode";
    private static final String TAG_ALLOW_OVERLAP = "allowOverlap";
    private static final String TAG_WAS_ACTIVE = "wasActiveLastTick";
    private static final String TAG_ACTIONS = "actions";

    /** Controls when a trigger whose condition holds actually fires its action sequence. */
    public enum ExecutionMode {
        /** Fires once when the condition goes from false to true; re-arms once it is false again. */
        ONCE_UNTIL_FALSE,
        /** Fires on every tick the condition is true. */
        CONTINUOUS
    }

    private final TriggerType<?> type;
    private UUID instanceId = UUID.randomUUID();
    private ExecutionMode mode = ExecutionMode.ONCE_UNTIL_FALSE;
    private boolean allowOverlap = false;
    private boolean wasActiveLastTick = false;
    private final List<TriggerAction> actions = new ArrayList<>();

    protected Trigger(TriggerType<?> type) {
        this.type = type;
    }

    /**
     * Evaluates whether this trigger's condition currently holds.
     *
     * @param ctx the per-tick world snapshot
     * @return {@code true} if the condition is met this tick
     */
    public abstract boolean evaluate(TriggerContext ctx);

    /** The registered type of this trigger. */
    public final TriggerType<?> getType() { return type; }

    /**
     * A stable identity for this configured trigger instance, used to re-link persisted
     * {@link ActionSequenceTracker}s to their trigger after a server restart.
     */
    public final UUID getInstanceId() { return instanceId; }

    public final ExecutionMode getMode() { return mode; }
    public final void setMode(ExecutionMode mode) { this.mode = mode; }

    /**
     * Whether this trigger may spawn a new action sequence while a previous one it
     * spawned is still running. When {@code false}, firings are skipped until the
     * running sequence finishes.
     */
    public final boolean allowsOverlap() { return allowOverlap; }
    public final void setAllowOverlap(boolean allowOverlap) { this.allowOverlap = allowOverlap; }

    /** Whether the condition held on the previous tick (edge detection for {@link ExecutionMode#ONCE_UNTIL_FALSE}). */
    public final boolean wasActiveLastTick() { return wasActiveLastTick; }

    /** Updated by the {@link TriggerManager} after each evaluation. */
    final void setWasActiveLastTick(boolean active) { this.wasActiveLastTick = active; }

    /** The ordered, mutable list of actions executed when this trigger fires. */
    public final List<TriggerAction> getActions() { return Collections.unmodifiableList(actions); }

    public final void addAction(TriggerAction action) { actions.add(action); }
    public final void removeAction(TriggerAction action) { actions.remove(action); }
    public final void clearActions() { actions.clear(); }

    // Disk serialization (NBT)

    /** Serializes this trigger, type ID first so {@link #createFromTag} can reconstruct it. */
    public final CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_TYPE, type.getId().toString());
        tag.putUUID(TAG_INSTANCE_ID, instanceId);
        tag.putString(TAG_MODE, mode.name());
        tag.putBoolean(TAG_ALLOW_OVERLAP, allowOverlap);
        tag.putBoolean(TAG_WAS_ACTIVE, wasActiveLastTick);

        ListTag actionList = new ListTag();
        for (TriggerAction action : actions) {
            actionList.add(action.save());
        }
        tag.put(TAG_ACTIONS, actionList);

        saveExtra(tag);
        return tag;
    }

    /** Subclass hook: write configuration data to disk. */
    protected void saveExtra(CompoundTag tag) {}

    /** Deserializes this trigger's configuration and edge state from disk. */
    public final void load(CompoundTag tag) {
        this.instanceId = tag.getUUID(TAG_INSTANCE_ID);
        this.mode = ExecutionMode.valueOf(tag.getString(TAG_MODE));
        this.allowOverlap = tag.getBoolean(TAG_ALLOW_OVERLAP);
        this.wasActiveLastTick = tag.getBoolean(TAG_WAS_ACTIVE);

        actions.clear();
        ListTag actionList = tag.getList(TAG_ACTIONS, Tag.TAG_COMPOUND);
        for (int i = 0; i < actionList.size(); i++) {
            actions.add(TriggerAction.createFromTag(actionList.getCompound(i)));
        }

        loadExtra(tag);
    }

    /** Subclass hook: read configuration data from disk. */
    protected void loadExtra(CompoundTag tag) {}

    /** Reconstructs the correct trigger subclass from a tag previously written by {@link #save}. */
    public static Trigger createFromTag(CompoundTag tag) {
        ResourceLocation typeId = new ResourceLocation(tag.getString(TAG_TYPE));
        Trigger trigger = TriggerRegistry.getOrThrow(typeId).create();
        trigger.load(tag);
        return trigger;
    }

    // Network serialization

    /** Serializes this trigger for network sync, type ID first so {@link #createFromBuf} can reconstruct it. */
    public final void writeSyncData(FriendlyByteBuf buf) {
        buf.writeResourceLocation(type.getId());
        buf.writeUUID(instanceId);
        buf.writeEnum(mode);
        buf.writeBoolean(allowOverlap);

        buf.writeVarInt(actions.size());
        for (TriggerAction action : actions) {
            action.writeSyncData(buf);
        }

        writeExtraSyncData(buf);
    }

    /** Subclass hook: write configuration data to the network buffer. */
    protected void writeExtraSyncData(FriendlyByteBuf buf) {}

    /** Deserializes this trigger's configuration from a network buffer. */
    public final void readSyncData(FriendlyByteBuf buf) {
        this.instanceId = buf.readUUID();
        this.mode = buf.readEnum(ExecutionMode.class);
        this.allowOverlap = buf.readBoolean();

        actions.clear();
        int actionCount = buf.readVarInt();
        for (int i = 0; i < actionCount; i++) {
            actions.add(TriggerAction.createFromBuf(buf));
        }

        readExtraSyncData(buf);
    }

    /** Subclass hook: read configuration data from the network buffer. */
    protected void readExtraSyncData(FriendlyByteBuf buf) {}

    /** Reconstructs the correct trigger subclass from a buffer previously written by {@link #writeSyncData}. */
    public static Trigger createFromBuf(FriendlyByteBuf buf) {
        ResourceLocation typeId = buf.readResourceLocation();
        Trigger trigger = TriggerRegistry.getOrThrow(typeId).create();
        trigger.readSyncData(buf);
        return trigger;
    }
}

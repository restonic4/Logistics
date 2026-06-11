package com.restonic4.logistics.blocks.computer.automation.triggers.core;

import com.restonic4.logistics.blocks.computer.automation.triggers.registry.ActionRegistry;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.ActionType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * A single configurable step inside a {@link Trigger}'s action sequence.
 * <p>
 * Actions are executed strictly in order by an {@link ActionSequenceTracker}. An action
 * never blocks the game tick: work that spans time (e.g. a delay) stores its progress in
 * {@link ActionExecutionContext#getActionState()} and returns {@link ExecuteResult#HOLD}
 * until it is done. Configuration data lives in the subclass and is (de)serialized via
 * the {@code saveExtra}/{@code loadExtra} and {@code writeExtraSyncData}/{@code readExtraSyncData}
 * hooks; runtime progress lives in the tracker, never in the action itself, so a single
 * configured action can safely back multiple overlapping sequences.
 */
public abstract class TriggerAction {
    private static final String TAG_TYPE = "type";

    private final ActionType<?> type;

    protected TriggerAction(ActionType<?> type) {
        this.type = type;
    }

    /** The registered type of this action. */
    public final ActionType<?> getType() { return type; }

    /**
     * Executes (or continues executing) this action for the current tick.
     *
     * @param ctx    the per-tick world snapshot
     * @param runCtx the runtime state of the owning sequence (scratch tag, action index)
     * @return how the owning sequence should proceed
     */
    public abstract ExecuteResult execute(TriggerContext ctx, ActionExecutionContext runCtx);

    // Disk serialization (NBT)

    /** Serializes this action, type ID first so {@link #createFromTag} can reconstruct it. */
    public final CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_TYPE, type.getId().toString());
        saveExtra(tag);
        return tag;
    }

    /** Subclass hook: write configuration data to disk. */
    protected void saveExtra(CompoundTag tag) {}

    /** Deserializes this action's configuration from disk. */
    public final void load(CompoundTag tag) {
        loadExtra(tag);
    }

    /** Subclass hook: read configuration data from disk. */
    protected void loadExtra(CompoundTag tag) {}

    /** Reconstructs the correct action subclass from a tag previously written by {@link #save}. */
    public static TriggerAction createFromTag(CompoundTag tag) {
        ResourceLocation typeId = new ResourceLocation(tag.getString(TAG_TYPE));
        TriggerAction action = ActionRegistry.getOrThrow(typeId).create();
        action.load(tag);
        return action;
    }

    // Network serialization

    /** Serializes this action for network sync, type ID first so {@link #createFromBuf} can reconstruct it. */
    public final void writeSyncData(FriendlyByteBuf buf) {
        buf.writeResourceLocation(type.getId());
        writeExtraSyncData(buf);
    }

    /** Subclass hook: write configuration data to the network buffer. */
    protected void writeExtraSyncData(FriendlyByteBuf buf) {}

    /** Deserializes this action's configuration from a network buffer. */
    public final void readSyncData(FriendlyByteBuf buf) {
        readExtraSyncData(buf);
    }

    /** Subclass hook: read configuration data from the network buffer. */
    protected void readExtraSyncData(FriendlyByteBuf buf) {}

    /** Reconstructs the correct action subclass from a buffer previously written by {@link #writeSyncData}. */
    public static TriggerAction createFromBuf(FriendlyByteBuf buf) {
        ResourceLocation typeId = buf.readResourceLocation();
        TriggerAction action = ActionRegistry.getOrThrow(typeId).create();
        action.readSyncData(buf);
        return action;
    }
}

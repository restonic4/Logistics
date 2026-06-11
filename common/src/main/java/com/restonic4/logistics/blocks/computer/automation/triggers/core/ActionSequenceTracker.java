package com.restonic4.logistics.blocks.computer.automation.triggers.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.UUID;

/**
 * A live run of a {@link Trigger}'s action sequence.
 * <p>
 * One tracker is spawned per firing and processed once per tick by the {@link TriggerManager}.
 * Within a single tick it advances through as many consecutive {@link ExecuteResult#SUCCESS}
 * actions as possible, parking on the current action whenever it returns
 * {@link ExecuteResult#HOLD} (e.g. a running delay) and aborting on {@link ExecuteResult#FAIL}.
 * Its position and the current action's scratch state are persisted to disk so in-flight
 * delays survive server restarts; on load it is re-linked to its trigger by instance ID.
 */
public final class ActionSequenceTracker implements ActionExecutionContext {
    private static final String TAG_TRIGGER_ID = "triggerId";
    private static final String TAG_ACTION_INDEX = "actionIndex";
    private static final String TAG_ACTION_STATE = "actionState";

    private final Trigger trigger;
    private int actionIndex;
    private CompoundTag actionState;

    /** Starts a fresh run of the given trigger's action list. */
    public ActionSequenceTracker(Trigger trigger) {
        this(trigger, 0, new CompoundTag());
    }

    private ActionSequenceTracker(Trigger trigger, int actionIndex, CompoundTag actionState) {
        this.trigger = trigger;
        this.actionIndex = actionIndex;
        this.actionState = actionState;
    }

    /**
     * Re-targets this run onto a replacement trigger instance (used when the user saves an
     * edited configuration and the trigger's action list is unchanged), preserving position
     * and scratch state.
     */
    ActionSequenceTracker retarget(Trigger replacement) {
        return new ActionSequenceTracker(replacement, actionIndex, actionState);
    }

    /** The trigger whose action list this tracker is running. */
    public Trigger getTrigger() { return trigger; }

    @Override
    public CompoundTag getActionState() { return actionState; }

    @Override
    public int getActionIndex() { return actionIndex; }

    /**
     * Processes this sequence for one game tick.
     *
     * @param ctx the per-tick world snapshot
     * @return {@code true} if the sequence is finished (completed or failed) and should be removed
     */
    public boolean tick(TriggerContext ctx) {
        List<TriggerAction> actions = trigger.getActions();

        while (actionIndex < actions.size()) {
            TriggerAction action = actions.get(actionIndex);
            ExecuteResult result = action.execute(ctx, this);

            switch (result) {
                case SUCCESS -> {
                    // Proceed to the next action immediately, with a fresh scratch state.
                    actionIndex++;
                    actionState = new CompoundTag();
                }
                case HOLD -> {
                    return false;
                }
                case FAIL -> {
                    return true;
                }
            }
        }

        return true;
    }

    // Disk serialization (NBT)

    /** Serializes this tracker's position and scratch state so active delays persist through restarts. */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(TAG_TRIGGER_ID, trigger.getInstanceId());
        tag.putInt(TAG_ACTION_INDEX, actionIndex);
        tag.put(TAG_ACTION_STATE, actionState);
        return tag;
    }

    /**
     * Reconstructs a tracker from disk, re-linking it to its trigger by instance ID.
     *
     * @param triggers the triggers currently configured on the owning manager
     * @return the restored tracker, or {@code null} if its trigger no longer exists
     */
    public static ActionSequenceTracker createFromTag(CompoundTag tag, List<Trigger> triggers) {
        UUID triggerId = tag.getUUID(TAG_TRIGGER_ID);
        for (Trigger trigger : triggers) {
            if (trigger.getInstanceId().equals(triggerId)) {
                return new ActionSequenceTracker(trigger, tag.getInt(TAG_ACTION_INDEX), tag.getCompound(TAG_ACTION_STATE));
            }
        }
        return null;
    }

    // Network serialization

    /** Serializes this tracker's position and scratch state for network sync. */
    public void writeSyncData(FriendlyByteBuf buf) {
        buf.writeUUID(trigger.getInstanceId());
        buf.writeVarInt(actionIndex);
        buf.writeNbt(actionState);
    }

    /**
     * Reconstructs a tracker from a network buffer, re-linking it to its trigger by instance ID.
     * The buffer is always fully consumed, even when the trigger no longer exists.
     *
     * @param triggers the triggers currently configured on the owning manager
     * @return the restored tracker, or {@code null} if its trigger no longer exists
     */
    public static ActionSequenceTracker createFromBuf(FriendlyByteBuf buf, List<Trigger> triggers) {
        UUID triggerId = buf.readUUID();
        int actionIndex = buf.readVarInt();
        CompoundTag actionState = buf.readNbt();
        if (actionState == null) actionState = new CompoundTag();

        for (Trigger trigger : triggers) {
            if (trigger.getInstanceId().equals(triggerId)) {
                return new ActionSequenceTracker(trigger, actionIndex, actionState);
            }
        }
        return null;
    }
}

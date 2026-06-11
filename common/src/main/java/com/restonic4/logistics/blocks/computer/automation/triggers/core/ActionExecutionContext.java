package com.restonic4.logistics.blocks.computer.automation.triggers.core;

import net.minecraft.nbt.CompoundTag;

/**
 * The mutable runtime state handed to a {@link TriggerAction} while it executes
 * inside an {@link ActionSequenceTracker}.
 * <p>
 * Actions that span multiple ticks (e.g. "Wait X Ticks") keep their progress in
 * {@link #getActionState()}. That tag is owned by the sequence tracker, reset
 * whenever the sequence advances to the next action, and persisted to disk so
 * in-flight delays survive server restarts.
 */
public interface ActionExecutionContext {
    /**
     * The scratch state of the currently executing action. Mutations are kept
     * across ticks while the action returns {@link ExecuteResult#HOLD} and are
     * discarded once the action completes.
     */
    CompoundTag getActionState();

    /** Index of the currently executing action within its trigger's action list. */
    int getActionIndex();
}

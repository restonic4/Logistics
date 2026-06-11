package com.restonic4.logistics.blocks.computer.automation.triggers.core;

/**
 * The result of a single {@link TriggerAction#execute} call, driving the
 * non-blocking sequential execution engine of an {@link ActionSequenceTracker}.
 */
public enum ExecuteResult {
    /** The action finished; the sequence proceeds to the next action immediately (same tick). */
    SUCCESS,
    /** The action is waiting (e.g. a timer delay); the sequence stays on this action and retries next tick. */
    HOLD,
    /** The action failed; the whole sequence is aborted. */
    FAIL
}

package com.restonic4.logistics.blocks.computer.automation.triggers.actions;

import com.restonic4.logistics.blocks.computer.automation.triggers.core.ActionExecutionContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.ExecuteResult;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerAction;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.ActionRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Pauses its sequence for a configured number of game ticks without blocking anything else.
 * <p>
 * The remaining tick count is kept in the sequence's {@link ActionExecutionContext#getActionState()
 * scratch state} — not in this action — so overlapping runs of the same trigger each count
 * down independently, and an in-flight wait resumes exactly where it left off after a
 * server restart.
 */
public class WaitTicksAction extends TriggerAction {
    private static final String TAG_WAIT_TICKS = "waitTicks";
    private static final String STATE_REMAINING = "remainingTicks";

    private int waitTicks = 20;

    public WaitTicksAction() {
        super(ActionRegistry.WAIT_TICKS);
    }

    /** The configured delay length in game ticks (20 ticks = 1 second). */
    public int getWaitTicks() { return waitTicks; }

    public void setWaitTicks(int waitTicks) {
        this.waitTicks = Math.max(0, waitTicks);
    }

    @Override
    public ExecuteResult execute(TriggerContext ctx, ActionExecutionContext runCtx) {
        CompoundTag state = runCtx.getActionState();

        int remaining = state.contains(STATE_REMAINING) ? state.getInt(STATE_REMAINING) : waitTicks;
        if (remaining <= 0) {
            return ExecuteResult.SUCCESS;
        }

        state.putInt(STATE_REMAINING, remaining - 1);
        return ExecuteResult.HOLD;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putInt(TAG_WAIT_TICKS, waitTicks);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        this.waitTicks = tag.getInt(TAG_WAIT_TICKS);
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        buf.writeVarInt(waitTicks);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        this.waitTicks = buf.readVarInt();
    }
}

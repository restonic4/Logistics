package com.restonic4.logistics.blocks.computer.automation.triggers.types;

import com.restonic4.logistics.blocks.computer.automation.triggers.core.Trigger;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.TriggerRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Fires on a fixed game-time interval (every N ticks). The condition is based on the
 * level's game time, so multiple computers configured with the same period fire on the
 * exact same tick — handy for keeping separate audio stations in sync.
 * <p>
 * With period 1 and {@link ExecutionMode#CONTINUOUS} this acts as an "always" trigger:
 * combined with overlap disabled, the action sequence re-runs in an endless loop
 * (e.g. a repeating playlist).
 */
public class IntervalTrigger extends Trigger {
    private static final String TAG_PERIOD = "periodTicks";

    private int periodTicks = 20;

    public IntervalTrigger() {
        super(TriggerRegistry.INTERVAL);
        // An interval is a pulse, not an edge — fire whenever the tick matches.
        setMode(ExecutionMode.CONTINUOUS);
    }

    /** The firing period in game ticks (20 ticks = 1 second). Minimum 1. */
    public int getPeriodTicks() { return periodTicks; }

    public void setPeriodTicks(int periodTicks) {
        this.periodTicks = Math.max(1, periodTicks);
    }

    @Override
    public boolean evaluate(TriggerContext ctx) {
        return ctx.getGameTime() % periodTicks == 0;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putInt(TAG_PERIOD, periodTicks);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        setPeriodTicks(tag.getInt(TAG_PERIOD));
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        buf.writeVarInt(periodTicks);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        setPeriodTicks(buf.readVarInt());
    }
}

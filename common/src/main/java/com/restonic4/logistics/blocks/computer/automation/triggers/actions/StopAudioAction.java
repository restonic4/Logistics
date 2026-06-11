package com.restonic4.logistics.blocks.computer.automation.triggers.actions;

import com.restonic4.logistics.blocks.audio_station.AudioStationNode;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.ActionExecutionContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.ExecuteResult;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerAction;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.ActionRegistry;
import com.restonic4.logistics.blocks.computer.automation.triggers.util.AudioStationTarget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Stops playback on the targeted audio station(s). Targeting "all stations" gives a
 * network-wide mute (e.g. when power drops below a threshold).
 * <p>
 * Note: a station configured with auto play + loop will restart itself on the next tick;
 * stations meant to be trigger-controlled should have auto play disabled.
 */
public class StopAudioAction extends TriggerAction {
    private final AudioStationTarget target = new AudioStationTarget();

    public StopAudioAction() {
        super(ActionRegistry.STOP_AUDIO);
    }

    public AudioStationTarget getTarget() { return target; }

    @Override
    public ExecuteResult execute(TriggerContext ctx, ActionExecutionContext runCtx) {
        for (AudioStationNode station : target.resolve(ctx)) {
            station.stopPlayback();
        }
        return ExecuteResult.SUCCESS;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        target.save(tag);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        target.load(tag);
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        target.write(buf);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        target.read(buf);
    }
}

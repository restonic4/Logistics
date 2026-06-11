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
 * Pauses its sequence until the targeted audio station(s) have stopped playing.
 * <p>
 * This is the playlist building block: <i>Play track A → Wait for audio → Play track B</i>
 * chains songs back to back without guessing durations in wait-ticks. It holds while any
 * targeted station is playing, so it never finishes against a seamlessly looping source —
 * use non-looping sounds for playlists.
 */
public class WaitAudioAction extends TriggerAction {
    private final AudioStationTarget target = new AudioStationTarget();

    public WaitAudioAction() {
        super(ActionRegistry.WAIT_AUDIO);
    }

    public AudioStationTarget getTarget() { return target; }

    @Override
    public ExecuteResult execute(TriggerContext ctx, ActionExecutionContext runCtx) {
        for (AudioStationNode station : target.resolve(ctx)) {
            if (station.isPlaying()) {
                return ExecuteResult.HOLD;
            }
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

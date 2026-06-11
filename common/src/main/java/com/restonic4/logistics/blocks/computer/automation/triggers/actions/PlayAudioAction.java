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
 * Starts playback on the targeted audio station(s), replacing whatever they were playing.
 * <p>
 * With an explicit sound the stations all play that sound; with no sound configured each
 * station plays its own configured audio. Because every targeted station starts on the
 * same tick, targeting "all stations" naturally keeps them in sync.
 */
public class PlayAudioAction extends TriggerAction {
    private static final String TAG_SOUND = "soundPath";

    private final AudioStationTarget target = new AudioStationTarget();
    /** The uploaded sound to play; empty means "each station's configured audio". */
    private String soundPath = "";

    public PlayAudioAction() {
        super(ActionRegistry.PLAY_AUDIO);
    }

    public AudioStationTarget getTarget() { return target; }

    public String getSoundPath() { return soundPath; }
    public void setSoundPath(String soundPath) {
        this.soundPath = soundPath == null ? "" : soundPath;
    }

    @Override
    public ExecuteResult execute(TriggerContext ctx, ActionExecutionContext runCtx) {
        for (AudioStationNode station : target.resolve(ctx)) {
            station.play(soundPath);
        }
        return ExecuteResult.SUCCESS;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        target.save(tag);
        tag.putString(TAG_SOUND, soundPath);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        target.load(tag);
        this.soundPath = tag.getString(TAG_SOUND);
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        target.write(buf);
        buf.writeUtf(soundPath);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        target.read(buf);
        this.soundPath = buf.readUtf();
    }
}

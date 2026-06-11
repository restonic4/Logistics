package com.restonic4.logistics.blocks.computer.automation.triggers.types;

import com.restonic4.logistics.blocks.audio_station.AudioStationNode;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.Trigger;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.TriggerRegistry;
import com.restonic4.logistics.blocks.computer.automation.triggers.util.AudioStationTarget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/**
 * Fires based on whether targeted audio stations are playing.
 * <p>
 * Typical uses with {@link ExecutionMode#ONCE_UNTIL_FALSE}:
 * <ul>
 *   <li>{@code STOPPED} — "when this station finishes its audio, do X" (chained playlists,
 *       follow-up announcements).</li>
 *   <li>{@code PLAYING} — "the moment any station starts playing, do X" (sync other
 *       stations, dim alarms, etc.).</li>
 * </ul>
 */
public class AudioStateTrigger extends Trigger {
    private static final String TAG_STATE = "state";

    /** The station state the condition checks for. */
    public enum State {
        /** Condition holds while at least one targeted station is playing. */
        PLAYING,
        /** Condition holds while no targeted station is playing (and at least one station exists). */
        STOPPED
    }

    private final AudioStationTarget target = new AudioStationTarget();
    private State state = State.STOPPED;

    public AudioStateTrigger() {
        super(TriggerRegistry.AUDIO_STATE);
    }

    public AudioStationTarget getTarget() { return target; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    @Override
    public boolean evaluate(TriggerContext ctx) {
        List<AudioStationNode> stations = target.resolve(ctx);
        if (stations.isEmpty()) return false;

        boolean anyPlaying = false;
        for (AudioStationNode station : stations) {
            if (station.isPlaying()) {
                anyPlaying = true;
                break;
            }
        }

        return switch (state) {
            case PLAYING -> anyPlaying;
            case STOPPED -> !anyPlaying;
        };
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        target.save(tag);
        tag.putString(TAG_STATE, state.name());
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        target.load(tag);
        this.state = State.valueOf(tag.getString(TAG_STATE));
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        target.write(buf);
        buf.writeEnum(state);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        target.read(buf);
        this.state = buf.readEnum(State.class);
    }
}

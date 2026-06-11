package com.restonic4.logistics.blocks.audio_station;

import com.restonic4.logistics.audio.ServerAudioManager;
import com.restonic4.logistics.audio.ServerAudioSource;
import com.restonic4.logistics.audio.ServerAudioStorage;
import com.restonic4.logistics.blocks.base.NameIdentifier;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.nodes.FacingNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.UUID;

/**
 * A network speaker. Holds a configured audio (path, volume, pitch, radius, loop) and
 * exposes a public {@link #play}/{@link #stopPlayback}/{@link #isPlaying} API used both by
 * its own auto-play behavior and by the computer's trigger system.
 * <p>
 * Behavior modes:
 * <ul>
 *   <li><b>Auto play ON</b>: the station keeps its configured audio going by itself —
 *       it starts on world load / config apply, and (when looping) restarts whenever it
 *       gets interrupted (e.g. after a power outage).</li>
 *   <li><b>Auto play OFF</b>: the station is fully passive and only plays when told to,
 *       which is what trigger-driven setups (playlists, alarms, synced stations) want.</li>
 * </ul>
 * Losing network energy always stops playback.
 */
public class AudioStationNode extends EnergyNode implements FacingNode, NameIdentifier {
    private static final String NBT_AUDIO_SOURCE = "AudioSourceId";
    private static final String NBT_AUDIO_PATH = "AudioPath";
    private static final String NBT_VOLUME = "Volume";
    private static final String NBT_PITCH = "Pitch";
    private static final String NBT_RADIUS = "Radius";
    private static final String NBT_LOOPING = "Looping";
    private static final String NBT_AUTO_PLAY = "AutoPlay";
    private static final String NBT_LEGACY_REDSTONE_MODE = "RedstoneMode";

    private Direction facing;
    private UUID audioSourceId;

    private String audioPath = "";
    private float volume = 1.0f;
    private float pitch = 1.0f;
    private float radius = 32.0f;
    private boolean looping = false;
    private boolean autoPlay = true;
    @Nullable private String name = null;

    public AudioStationNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        EnergyNetwork energyNetwork = getNetwork();
        long energy = energyNetwork.requestEnergyConsumption(1);
        if (energy < 1) {
            stopPlayback();
            return;
        }

        // Drop the handle once the source has actually finished, so isPlaying() and the
        // synced client state report Idle instead of pointing at a dead source.
        if (audioSourceId != null) {
            ServerAudioSource source = ServerAudioManager.getSource(audioSourceId);
            if (source == null || source.isStopped()) {
                audioSourceId = null;
                setNetworkDirty();
            }
        }

        if (autoPlay && looping && !isPlaying() && !audioPath.isEmpty()) {
            play("");
        }

        if (audioSourceId != null) {
            ServerAudioManager.tickSource(audioSourceId, (ServerLevel) getLevel(), getBlockPos());
        }
    }

    @Override
    public void onInit() {
        if (getLevel() == null) return;
        if (getLevel().isClientSide()) return;
        if (audioPath.isEmpty()) return;
        if (!autoPlay) return;
        play("");
    }

    @Override
    public void onRemove() {
        stopPlayback();
    }

    /**
     * Starts playback on this station, replacing whatever is currently playing.
     * Server side only.
     *
     * @param overridePath the uploaded sound to play, or empty/null to play the
     *                     station's configured audio
     */
    public void play(String overridePath) {
        if (getLevel() == null || getLevel().isClientSide()) return;

        String path = (overridePath == null || overridePath.isEmpty()) ? audioPath : overridePath;
        if (path.isEmpty()) return;

        File file = ServerAudioStorage.getSoundFile(path);
        if (file == null || !file.exists()) {
            System.err.println("Missing audio file: " + path);
            return;
        }

        if (audioSourceId != null) {
            ServerAudioManager.stop(audioSourceId);
        }
        audioSourceId = ServerAudioManager.play(
                (ServerLevel) getLevel(), getBlockPos(), file.getAbsolutePath(),
                volume, pitch, radius, looping
        );
        setNetworkDirty();
    }

    /** Stops whatever this station is playing. Server side only. */
    public void stopPlayback() {
        if (getLevel() == null || getLevel().isClientSide()) return;
        if (this.audioSourceId != null) {
            ServerAudioManager.stop(this.audioSourceId);
            this.audioSourceId = null;
            setNetworkDirty();
        }
    }

    /**
     * Whether this station is currently playing. On the server this checks the live
     * audio source; on the client it reflects the last synced state.
     */
    public boolean isPlaying() {
        if (audioSourceId == null) return false;
        if (getLevel() == null || getLevel().isClientSide()) return true;
        ServerAudioSource source = ServerAudioManager.getSource(audioSourceId);
        return source != null && !source.isStopped();
    }

    public void applyConfig(String audioPath, float volume, float pitch, float radius, boolean looping, boolean autoPlay) {
        boolean pathChanged = !this.audioPath.equals(audioPath);
        boolean loopChanged = this.looping != looping;

        if (!pathChanged && this.volume == volume && this.pitch == pitch && this.radius == radius
                && !loopChanged && this.autoPlay == autoPlay) {
            return;
        }

        this.audioPath = audioPath;
        this.volume = volume;
        this.pitch = pitch;
        this.radius = radius;
        this.looping = looping;
        this.autoPlay = autoPlay;

        if (pathChanged || loopChanged) {
            // The playing source no longer matches the configuration; restart below if needed.
            stopPlayback();
        } else if (isPlaying()) {
            ServerAudioManager.updateVolume(audioSourceId, volume);
            ServerAudioManager.updatePitch(audioSourceId, pitch);
            ServerAudioManager.updateRadius(audioSourceId, radius);
        }

        if (this.autoPlay && !isPlaying() && !this.audioPath.isEmpty()) {
            play("");
        }

        setNetworkDirty();
    }

    public String getAudioPath() { return audioPath; }
    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }
    public float getRadius() { return radius; }
    public boolean isLooping() { return looping; }
    public boolean isAutoPlay() { return autoPlay; }

    @Override
    public void setName(@NotNull String name) {
        this.onNameChange(this.name, name, this);
        this.name = name;
    }

    @Override
    public @Nullable String getName() {
        return name;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        this.saveFacing(tag);
        this.saveName(tag);
        tag.putString(NBT_AUDIO_PATH, audioPath);
        tag.putFloat(NBT_VOLUME, volume);
        tag.putFloat(NBT_PITCH, pitch);
        tag.putFloat(NBT_RADIUS, radius);
        tag.putBoolean(NBT_LOOPING, looping);
        tag.putBoolean(NBT_AUTO_PLAY, autoPlay);
        if (this.audioSourceId != null) {
            tag.putUUID(NBT_AUDIO_SOURCE, this.audioSourceId);
        }
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        this.loadFacing(tag);
        this.loadName(tag);
        this.audioPath = tag.getString(NBT_AUDIO_PATH);
        this.volume = tag.getFloat(NBT_VOLUME);
        this.pitch = tag.getFloat(NBT_PITCH);
        this.radius = tag.getFloat(NBT_RADIUS);
        this.looping = tag.getBoolean(NBT_LOOPING);
        if (tag.contains(NBT_AUTO_PLAY)) {
            this.autoPlay = tag.getBoolean(NBT_AUTO_PLAY);
        } else {
            // Legacy saves: non-redstone stations behaved like auto play.
            this.autoPlay = !tag.getBoolean(NBT_LEGACY_REDSTONE_MODE);
        }
        if (tag.hasUUID(NBT_AUDIO_SOURCE)) {
            this.audioSourceId = tag.getUUID(NBT_AUDIO_SOURCE);
        }
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        super.writeExtraSyncData(buf);
        this.writeFacing(buf);
        this.writeName(buf);
        buf.writeUtf(audioPath);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
        buf.writeFloat(radius);
        buf.writeBoolean(looping);
        buf.writeBoolean(autoPlay);
        buf.writeBoolean(this.audioSourceId != null);
        if (this.audioSourceId != null) {
            buf.writeUUID(audioSourceId);
        }
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        super.readExtraSyncData(buf);
        this.readFacing(buf);
        this.readName(buf);
        this.audioPath = buf.readUtf();
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
        this.radius = buf.readFloat();
        this.looping = buf.readBoolean();
        this.autoPlay = buf.readBoolean();
        boolean hasAudioSourceId = buf.readBoolean();
        this.audioSourceId = hasAudioSourceId ? buf.readUUID() : null;
    }

    @Override
    public void setFacing(@NotNull Direction facing) {
        this.onFacingChange(this.facing, facing, this);
        this.facing = facing;
    }

    @Override
    public @Nullable Direction getFacing() { return facing; }

    @Override
    public boolean buildScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        boolean added = super.buildScannerTooltip(builder, isSneaking);
        boolean nameAdditions = this.buildNameScannerTooltip(builder, isSneaking);

        return added || nameAdditions;
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildDebugScannerTooltip(builder, isSneaking);
        builder.keyValue("Facing", facing != null ? facing.getSerializedName() : "null", ChatFormatting.YELLOW);
        builder.keyValue("Audio", this.audioSourceId != null ? "Active" : "Inactive", ChatFormatting.GREEN);
        builder.keyValue("Path", this.audioPath.isEmpty() ? "None" : this.audioPath, ChatFormatting.YELLOW);
        builder.keyValue("Mode", this.autoPlay ? "Auto play" : "Manual", ChatFormatting.YELLOW);
        builder.keyValue("Audios", String.valueOf(ServerAudioStorage.getAllSounds().size()), ChatFormatting.GOLD);
        return true;
    }
}

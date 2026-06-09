package com.restonic4.logistics.blocks.audio_station;

import com.restonic4.logistics.audio.ServerAudioManager;
import com.restonic4.logistics.audio.ServerAudioStorage;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.nodes.FacingNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.UUID;

public class AudioStationNode extends EnergyNode implements FacingNode {
    private static final String NBT_AUDIO_SOURCE = "AudioSourceId";
    private static final String NBT_AUDIO_PATH = "AudioPath";
    private static final String NBT_VOLUME = "Volume";
    private static final String NBT_PITCH = "Pitch";
    private static final String NBT_RADIUS = "Radius";
    private static final String NBT_LOOPING = "Looping";
    private static final String NBT_REDSTONE_MODE = "RedstoneMode";

    private Direction facing;
    private UUID audioSourceId;

    private String audioPath = "";
    private float volume = 1.0f;
    private float pitch = 1.0f;
    private float radius = 32.0f;
    private boolean looping = false;
    private boolean redstoneMode = false;

    private boolean wasPowered = false;

    public AudioStationNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        EnergyNetwork energyNetwork = getNetwork();
        long energy = energyNetwork.requestEnergyConsumption(1);
        if (energy < 1) {
            stopAudio();
            return;
        }

        ServerLevel level = (ServerLevel) getLevel();
        BlockPos pos = getBlockPos();
        //boolean powered = level.hasNeighborSignal(pos);

        if (redstoneMode) {
            /*if (powered && !wasPowered) {
                playAudio();
            }
            if (audioSourceId != null) {
                ServerAudioManager.tickSource(audioSourceId, level, pos);
            }*/
        } else {
            if (looping) {
                if (audioSourceId == null || ServerAudioManager.getSource(audioSourceId) == null
                        || ServerAudioManager.getSource(audioSourceId).isStopped()) {
                    if (!audioPath.isEmpty()) playAudio();
                }
            }
            if (audioSourceId != null) {
                ServerAudioManager.tickSource(audioSourceId, level, pos);
            }
        }

        //wasPowered = powered;
    }

    @Override
    public void onInit() {
        if (getLevel() == null) return;
        if (getLevel().isClientSide()) return;
        if (audioPath.isEmpty()) return;
        if (redstoneMode) return;
        playAudio();
    }

    @Override
    public void onRemove() {
        stopAudio();
    }

    private void playAudio() {
        if (audioPath.isEmpty()) return;
        File file = ServerAudioStorage.getSoundFile(audioPath);
        if (file == null || !file.exists()) {
            System.err.println("Missing audio file: " + audioPath);
            return;
        }
        if (audioSourceId != null) {
            ServerAudioManager.stop(audioSourceId);
        }
        audioSourceId = ServerAudioManager.play(
                (ServerLevel) getLevel(), getBlockPos(), file.getAbsolutePath(),
                volume, pitch, radius, looping
        );
    }

    private void stopAudio() {
        if (!getLevel().isClientSide() && this.audioSourceId != null) {
            ServerAudioManager.stop(this.audioSourceId);
            this.audioSourceId = null;
        }
    }

    public void applyConfig(String audioPath, float volume, float pitch, float radius,
                            boolean looping, boolean redstoneMode) {
        boolean pathChanged = !this.audioPath.equals(audioPath);
        boolean modeChanged = this.looping != looping || this.redstoneMode != redstoneMode;

        if (!pathChanged && this.volume == volume && this.pitch == pitch
                && this.radius == radius && !modeChanged) {
            return;
        }

        this.audioPath = audioPath;
        this.volume = volume;
        this.pitch = pitch;
        this.radius = radius;
        this.looping = looping;
        this.redstoneMode = redstoneMode;

        if (audioSourceId != null && (pathChanged || modeChanged)) {
            ServerAudioManager.stop(audioSourceId);
            audioSourceId = null;
        }

        if (audioSourceId != null && ServerAudioManager.getSource(audioSourceId) != null) {
            ServerAudioManager.updateVolume(audioSourceId, volume);
            ServerAudioManager.updatePitch(audioSourceId, pitch);
            ServerAudioManager.updateRadius(audioSourceId, radius);
        }

        if (!redstoneMode && pathChanged && !audioPath.isEmpty()) {
            playAudio();
        }
    }

    public String getAudioPath() { return audioPath; }
    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }
    public float getRadius() { return radius; }
    public boolean isLooping() { return looping; }
    public boolean isRedstoneMode() { return redstoneMode; }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        this.saveFacing(tag);
        tag.putString(NBT_AUDIO_PATH, audioPath);
        tag.putFloat(NBT_VOLUME, volume);
        tag.putFloat(NBT_PITCH, pitch);
        tag.putFloat(NBT_RADIUS, radius);
        tag.putBoolean(NBT_LOOPING, looping);
        tag.putBoolean(NBT_REDSTONE_MODE, redstoneMode);
        if (this.audioSourceId != null) {
            tag.putUUID(NBT_AUDIO_SOURCE, this.audioSourceId);
        }
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        this.loadFacing(tag);
        this.audioPath = tag.getString(NBT_AUDIO_PATH);
        this.volume = tag.getFloat(NBT_VOLUME);
        this.pitch = tag.getFloat(NBT_PITCH);
        this.radius = tag.getFloat(NBT_RADIUS);
        this.looping = tag.getBoolean(NBT_LOOPING);
        this.redstoneMode = tag.getBoolean(NBT_REDSTONE_MODE);
        if (tag.hasUUID(NBT_AUDIO_SOURCE)) {
            this.audioSourceId = tag.getUUID(NBT_AUDIO_SOURCE);
        }
    }

    @Override
    public void setFacing(@NotNull Direction facing) {
        this.onFacingChange(this.facing, facing, this);
        this.facing = facing;
    }

    @Override
    public @Nullable Direction getFacing() {
        return facing;
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildDebugScannerTooltip(builder, isSneaking);
        builder.keyValue("Facing", facing != null ? facing.getSerializedName() : "null", ChatFormatting.YELLOW);
        builder.keyValue("Audio", this.audioSourceId != null ? "Active" : "Inactive", ChatFormatting.GREEN);
        builder.keyValue("Path", this.audioPath.isEmpty() ? "None" : this.audioPath, ChatFormatting.YELLOW);
        builder.keyValue("Mode", this.redstoneMode ? "Redstone" : "Continuous", ChatFormatting.YELLOW);
        return true;
    }

    public record AudioStationData(BlockPos pos, String audioPath, float volume, float pitch, float radius, boolean looping, boolean redstoneMode) {}
}
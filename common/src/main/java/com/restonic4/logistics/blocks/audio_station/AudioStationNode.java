package com.restonic4.logistics.blocks.audio_station;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.audio.AudioFormat;
import com.restonic4.logistics.audio.server.ServerAudioManager;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.nodes.FacingNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AudioStationNode extends EnergyNode implements FacingNode {
    private Direction facing;
    private UUID audioSourceId;

    public AudioStationNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        if (this.audioSourceId == null) {
            startRadio();
        }
    }

    @Override
    public void onRemove() {
        stopRadio();
    }

    public void startRadio() {
        if (this.audioSourceId != null) return;

        Constants.LOG.info("Playing stream");

        Vec3 center = Vec3.atCenterOf(this.getBlockPos());
        this.audioSourceId = ServerAudioManager.getInstance().playRadio(
                getNetwork().getServerLevel(),
                center,
                "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_ASO_JEREZ.mp3",
                AudioFormat.MP3,
                1.0f, // volume
                1.0f, // pitch
                32.0f // radius in blocks
        );
    }

    public void stopRadio() {
        if (this.audioSourceId != null) {
            ServerAudioManager.getInstance().stop(this.audioSourceId);
            this.audioSourceId = null;
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
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        this.saveFacing(tag);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        this.loadFacing(tag);
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildDebugScannerTooltip(builder, isSneaking);

        builder.keyValue("Facing", facing.getSerializedName(), ChatFormatting.YELLOW);
        builder.keyValue("Streaming", audioSourceId != null ? "Active" : "Stopped", ChatFormatting.GREEN);

        return true;
    }
}

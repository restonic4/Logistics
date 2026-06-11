package com.restonic4.logistics.blocks.audio_station;

import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Manual playback control from the computer's Audio tab: play or stop a single station,
 * or stop every station on the computer's network at once.
 */
public class AudioStationControlPacket implements C2SPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio_station/control");

    public enum Action {
        PLAY,
        STOP,
        STOP_ALL
    }

    private final BlockPos targetPos;
    private final Action action;

    /**
     * @param targetPos the station to control, or the computer's position for {@link Action#STOP_ALL}
     */
    public AudioStationControlPacket(BlockPos targetPos, Action action) {
        this.targetPos = targetPos;
        this.action = action;
    }

    public AudioStationControlPacket(FriendlyByteBuf buf) {
        this.targetPos = buf.readBlockPos();
        this.action = buf.readEnum(Action.class);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(targetPos);
        buf.writeEnum(action);
    }

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        NetworkNode node = NetworkManager.get(player.serverLevel()).getNodeByBlockPos(targetPos);
        if (node == null) return;

        switch (action) {
            case PLAY -> {
                if (node instanceof AudioStationNode station) station.play("");
            }
            case STOP -> {
                if (node instanceof AudioStationNode station) station.stopPlayback();
            }
            case STOP_ALL -> {
                if (node.getNetwork() instanceof EnergyNetwork network) {
                    for (AudioStationNode station : network.getAudioStations()) {
                        station.stopPlayback();
                    }
                }
            }
        }
    }
}

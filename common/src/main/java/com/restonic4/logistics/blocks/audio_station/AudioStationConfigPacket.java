package com.restonic4.logistics.blocks.audio_station;

import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class AudioStationConfigPacket implements C2SPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio_station/config");

    private final BlockPos stationPos;
    private final String audioPath;
    private final float volume;
    private final float pitch;
    private final float radius;
    private final boolean looping;
    private final boolean autoPlay;

    public AudioStationConfigPacket(
            BlockPos stationPos, String audioPath,
            float volume, float pitch, float radius,
            boolean looping, boolean autoPlay
    ) {
        this.stationPos = stationPos;
        this.audioPath = audioPath;
        this.volume = volume;
        this.pitch = pitch;
        this.radius = radius;
        this.looping = looping;
        this.autoPlay = autoPlay;
    }

    public AudioStationConfigPacket(FriendlyByteBuf buf) {
        this.stationPos = buf.readBlockPos();
        this.audioPath = buf.readUtf();
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
        this.radius = buf.readFloat();
        this.looping = buf.readBoolean();
        this.autoPlay = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(stationPos);
        buf.writeUtf(audioPath);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
        buf.writeFloat(radius);
        buf.writeBoolean(looping);
        buf.writeBoolean(autoPlay);
    }

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        NetworkNode node = NetworkManager.get(player.serverLevel()).getNodeByBlockPos(stationPos);
        if (node instanceof AudioStationNode audioNode) {
            audioNode.applyConfig(audioPath, volume, pitch, radius, looping, autoPlay);
        }
    }
}

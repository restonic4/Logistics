package com.restonic4.logistics.blocks.audio_station;

import com.restonic4.logistics.audio.ServerAudioStorage;
import com.restonic4.logistics.networking.C2SPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class AudioDeletePacket implements C2SPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio_station/delete");

    private final String soundId;

    public AudioDeletePacket(String soundId) { this.soundId = soundId; }

    public AudioDeletePacket(FriendlyByteBuf buf) { this.soundId = buf.readUtf(); }

    @Override
    public void write(FriendlyByteBuf buf) { buf.writeUtf(soundId); }

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        if (soundId == null || !soundId.contains("/")) return;
        String owner = soundId.substring(0, soundId.indexOf('/'));
        if (!owner.equals(player.getUUID().toString())) {
            System.err.println("Player " + player.getUUID() + " tried to delete sound " + soundId);
            return;
        }
        ServerAudioStorage.deleteSound(soundId);
    }
}
package com.restonic4.logistics.audio;

import com.restonic4.logistics.audio.ClientAudioManager;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class AudioStopS2CPacket implements S2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio/stop");

    private final UUID sourceId;

    public AudioStopS2CPacket(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public AudioStopS2CPacket(FriendlyByteBuf buf) {
        this.sourceId = buf.readUUID();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.sourceId);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> ClientAudioManager.stop(this.sourceId));
    }
}
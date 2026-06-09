package com.restonic4.logistics.audio;

import com.restonic4.logistics.audio.ClientAudioManager;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class AudioUpdateS2CPacket implements S2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio/update");

    private final UUID sourceId;
    private final float volume;
    private final float pitch;
    private final float radius;

    public AudioUpdateS2CPacket(UUID sourceId, float volume, float pitch, float radius) {
        this.sourceId = sourceId;
        this.volume = volume;
        this.pitch = pitch;
        this.radius = radius;
    }

    public AudioUpdateS2CPacket(FriendlyByteBuf buf) {
        this.sourceId = buf.readUUID();
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
        this.radius = buf.readFloat();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.sourceId);
        buf.writeFloat(this.volume);
        buf.writeFloat(this.pitch);
        buf.writeFloat(this.radius);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> ClientAudioManager.update(this.sourceId, this.volume, this.pitch, this.radius));
    }
}
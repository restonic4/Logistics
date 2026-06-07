package com.restonic4.logistics.audio.network;

import com.restonic4.logistics.audio.AudioFormat;
import com.restonic4.logistics.audio.client.ClientAudioManager;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class ClientboundAudioPlayPacket implements S2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio_play");

    private final UUID sourceId;
    private final Vec3 position;
    private final ResourceLocation soundId;
    private final String url;
    private final AudioFormat format;
    private final boolean isRadio;
    private final float volume;
    private final float pitch;
    private final float radius;
    private final long elapsedMs;

    public ClientboundAudioPlayPacket(UUID sourceId, Vec3 position, ResourceLocation soundId, String url,
                                      AudioFormat format, boolean isRadio, float volume, float pitch,
                                      float radius, long elapsedMs) {
        this.sourceId = sourceId;
        this.position = position;
        this.soundId = soundId;
        this.url = url;
        this.format = format;
        this.isRadio = isRadio;
        this.volume = volume;
        this.pitch = pitch;
        this.radius = radius;
        this.elapsedMs = elapsedMs;
    }

    public ClientboundAudioPlayPacket(FriendlyByteBuf buf) {
        this.sourceId = buf.readUUID();
        this.position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.isRadio = buf.readBoolean();
        this.url = buf.readUtf(32767);
        this.soundId = buf.readBoolean() ? buf.readResourceLocation() : null;
        this.format = buf.readEnum(AudioFormat.class);
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
        this.radius = buf.readFloat();
        this.elapsedMs = buf.readLong();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(sourceId);
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
        buf.writeBoolean(isRadio);
        buf.writeUtf(url);
        buf.writeBoolean(soundId != null);
        if (soundId != null) buf.writeResourceLocation(soundId);
        buf.writeEnum(format);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
        buf.writeFloat(radius);
        buf.writeLong(elapsedMs);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> ClientAudioManager.getInstance().handlePlay(this));
    }

    public UUID getSourceId() { return sourceId; }
    public Vec3 getPosition() { return position; }
    public ResourceLocation getSoundId() { return soundId; }
    public String getUrl() { return url; }
    public AudioFormat getFormat() { return format; }
    public boolean isRadio() { return isRadio; }
    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }
    public float getRadius() { return radius; }
    public long getElapsedMs() { return elapsedMs; }
}
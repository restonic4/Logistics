package com.restonic4.logistics.audio;

import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Tells the client to start (or restart) a positional audio source. Carries the logical
 * {@code soundId} ("uuid/file.wav") plus a content {@code hash} instead of a server file
 * path, so the client can resolve the audio from its own cache and download it on demand —
 * this is what makes playback work on remote dedicated servers.
 */
public class AudioPlayS2CPacket implements S2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio/play");

    private final UUID sourceId;
    private final Vec3 pos;
    private final String soundId;
    private final String hash;
    private final float volume;
    private final float pitch;
    private final float radius;
    private final long elapsedMs;
    private final boolean looping;

    public AudioPlayS2CPacket(UUID sourceId, Vec3 pos, String soundId, String hash, float volume,
                              float pitch, float radius, long elapsedMs, boolean looping) {
        this.sourceId = sourceId;
        this.pos = pos;
        this.soundId = soundId;
        this.hash = hash;
        this.volume = volume;
        this.pitch = pitch;
        this.radius = radius;
        this.elapsedMs = elapsedMs;
        this.looping = looping;
    }

    public AudioPlayS2CPacket(FriendlyByteBuf buf) {
        this.sourceId = buf.readUUID();
        this.pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.soundId = buf.readUtf();
        this.hash = buf.readUtf();
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
        this.radius = buf.readFloat();
        this.elapsedMs = buf.readLong();
        this.looping = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.sourceId);
        buf.writeDouble(this.pos.x);
        buf.writeDouble(this.pos.y);
        buf.writeDouble(this.pos.z);
        buf.writeUtf(this.soundId);
        buf.writeUtf(this.hash);
        buf.writeFloat(this.volume);
        buf.writeFloat(this.pitch);
        buf.writeFloat(this.radius);
        buf.writeLong(this.elapsedMs);
        buf.writeBoolean(this.looping);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> {
            ClientAudioManager.play(this.sourceId, this.pos, this.soundId, this.hash,
                    this.volume, this.pitch, this.radius, this.elapsedMs, this.looping);
        });
    }
}

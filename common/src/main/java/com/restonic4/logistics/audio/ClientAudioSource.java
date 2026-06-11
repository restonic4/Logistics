package com.restonic4.logistics.audio;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import com.restonic4.logistics.mixin.audio.ChannelAccessor;
import com.restonic4.logistics.mixin.audio.SoundEngineEx;
import com.restonic4.logistics.mixin.audio.SoundManagerEx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import java.util.UUID;

public class ClientAudioSource {
    private final UUID id;
    private Vec3 pos;
    private final String filePath;
    private volatile float baseVolume;
    private volatile float pitch;
    private volatile float radius;
    private final boolean looping;
    private long elapsedMs;
    private long lastTickTime;
    private volatile ChannelAccess.ChannelHandle channelHandle;
    private volatile boolean released;

    public ClientAudioSource(
            UUID id, Vec3 pos, String filePath, float volume,
            float pitch, float radius, long elapsedMs, boolean looping
    ) {
        this.id = id;
        this.pos = pos;
        this.filePath = filePath;
        this.baseVolume = volume;
        this.pitch = pitch;
        this.radius = radius;
        this.looping = looping;
        this.elapsedMs = elapsedMs;
        this.lastTickTime = System.currentTimeMillis();
    }

    public void play() {
        if (released) return;
        final float initialGain = computeAttenuation();

        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        SoundEngine soundEngine = ((SoundManagerEx) soundManager).logistics$getSoundEngine();
        ChannelAccess channelAccess = ((SoundEngineEx) soundEngine).logistics$getChannelAccess();

        channelAccess.createHandle(Library.Pool.STATIC).thenAccept(handle -> {
            if (released || handle == null) {
                if (handle != null) {
                    handle.execute((Channel channel) -> {
                        int source = ((ChannelAccessor) channel).logistics$getSource();
                        AL10.alSourceStop(source);
                    });
                }
                return;
            }

            this.channelHandle = handle;

            handle.execute((Channel channel) -> {
                if (released) return;

                final AudioBuffer buffer;
                try {
                    buffer = AudioBufferCache.getOrLoad(this.filePath);
                } catch (Exception e) {
                    System.err.println("Failed to load audio on client: " + this.filePath);
                    e.printStackTrace();
                    return;
                }

                int source = ((ChannelAccessor) channel).logistics$getSource();

                AL10.alSourceStop(source);
                AL10.alSourcei(source, AL10.AL_BUFFER, 0);
                AL10.alSourcei(source, AL10.AL_LOOPING, this.looping ? AL10.AL_TRUE : AL10.AL_FALSE);

                AL10.alSourcei(source, AL10.AL_BUFFER, buffer.getAlBufferId());
                AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
                AL10.alSource3f(source, AL10.AL_POSITION,
                        (float) this.pos.x, (float) this.pos.y, (float) this.pos.z);

                AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 1.0f);
                AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, Float.MAX_VALUE);
                AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 0.0f);

                AL10.alSourcef(source, AL10.AL_PITCH, this.pitch);
                // Seek to the server-reported position so players (re)entering the radius
                // join mid-playback instead of hearing the audio restart. For looping
                // sources the server already sends the offset modulo the duration.
                if (this.elapsedMs > 0) {
                    AL11.alSourcef(source, AL11.AL_SEC_OFFSET, this.elapsedMs / 1000.0f);
                }

                AL10.alSourcef(source, AL10.AL_GAIN, initialGain);
                AL10.alSourcePlay(source);
            });
        });
    }

    public void stop() {
        released = true;
        ChannelAccess.ChannelHandle handle = this.channelHandle;
        this.channelHandle = null;
        if (handle != null) {
            handle.execute((Channel channel) -> {
                int source = ((ChannelAccessor) channel).logistics$getSource();
                AL10.alSourceStop(source);
                AL10.alSourcei(source, AL10.AL_BUFFER, 0);
            });
        }
    }

    public void update(float volume, float pitch, float radius) {
        this.baseVolume = volume;
        this.pitch = pitch;
        this.radius = radius;
        applyAttenuation();
    }

    public void tick() {
        long now = System.currentTimeMillis();
        if (!released && channelHandle != null) {
            this.elapsedMs += (now - this.lastTickTime);
            applyAttenuation();
        }
        this.lastTickTime = now;
    }

    private float computeAttenuation() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return this.baseVolume;
        double dist = Math.sqrt(mc.player.distanceToSqr(this.pos));
        if (dist >= this.radius) return 0.0f;
        float attenuation = 1.0f - (float) (dist / this.radius);
        return this.baseVolume * Math.max(0.0f, Math.min(1.0f, attenuation));
    }

    private void applyAttenuation() {
        float finalVolume = computeAttenuation();
        ChannelAccess.ChannelHandle handle = this.channelHandle;
        if (handle != null) {
            handle.execute((Channel channel) -> {
                int source = ((ChannelAccessor) channel).logistics$getSource();
                AL10.alSourcef(source, AL10.AL_GAIN, finalVolume);
                AL10.alSourcef(source, AL10.AL_PITCH, this.pitch);
                AL10.alSource3f(source, AL10.AL_POSITION,
                        (float) this.pos.x, (float) this.pos.y, (float) this.pos.z);
            });
        }
    }

    public UUID getId() { return id; }
    public boolean isReleased() { return released; }
}
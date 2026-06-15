package com.restonic4.logistics.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * A positional audio-station sound played through Minecraft's own sound engine, which owns the
 * OpenAL channel lifecycle (acquire/release) — so unlike the old hand-managed approach this
 * can never leak or exhaust channels and kill the engine.
 * <p>
 * Volume is fully decoupled from radius: attenuation is {@link Attenuation#NONE} (no vanilla
 * distance rolloff) and {@link #getVolume()} applies our own linear falloff over the configured
 * radius. The actual audio bytes are supplied by {@link com.restonic4.logistics.mixin.audio.SoundBufferLibraryMixin}
 * via {@link ClientAudioManager}'s playback registry.
 */
public class LogisticsSoundInstance extends AbstractTickableSoundInstance {
    private volatile float baseVolume;
    private volatile float radiusBlocks;

    public LogisticsSoundInstance(ResourceLocation location, Vec3 pos, float volume,
                                  float pitch, float radius, boolean looping) {
        super(SoundEvent.createVariableRangeEvent(location), SoundSource.RECORDS, RandomSource.create());
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.baseVolume = volume;
        this.pitch = pitch;
        this.radiusBlocks = radius;
        this.looping = looping;
        this.attenuation = Attenuation.NONE;
        this.relative = false;
    }

    /**
     * Builds the sound event ourselves so the engine never tries (and fails) to look our
     * runtime sound up in the sounds.json registry.
     */
    @Override
    public WeighedSoundEvents resolve(SoundManager soundManager) {
        this.sound = new Sound(
                this.location.toString(),
                rnd -> 1.0f,            // volume provider unused: getVolume() is overridden
                rnd -> 1.0f,            // pitch provider unused: getPitch() is overridden
                1,                      // weight
                Sound.Type.FILE,
                true,                   // stream: routes through SoundBufferLibrary.getStream
                false,                  // preload
                16                      // attenuation distance (unused with Attenuation.NONE)
        );
        WeighedSoundEvents events = new WeighedSoundEvents(this.location, null);
        events.addSound(this.sound);
        return events;
    }

    @Override
    public float getVolume() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || radiusBlocks <= 0) return 0.0f;
        double dist = Math.sqrt(mc.player.distanceToSqr(this.x, this.y, this.z));
        if (dist >= radiusBlocks) return 0.0f;
        float attenuation = 1.0f - (float) (dist / radiusBlocks);
        return baseVolume * Math.max(0.0f, Math.min(1.0f, attenuation));
    }

    @Override
    public float getPitch() {
        return this.pitch;
    }

    // We start sources the moment a player enters the radius, where falloff is ~0; allow that
    // so the engine doesn't skip them as "volume was zero" before they ramp up.
    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        // Volume is recomputed by the engine each tick via getVolume(); nothing else to do.
    }

    public void updateConfig(float volume, float pitch, float radius) {
        this.baseVolume = volume;
        this.pitch = pitch;
        this.radiusBlocks = radius;
    }
}

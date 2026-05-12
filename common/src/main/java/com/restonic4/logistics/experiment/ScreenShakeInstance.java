package com.restonic4.logistics.experiment;

import net.minecraft.client.Camera;

/**
 * Runtime state for one active screen shake.
 */
public abstract class ScreenShakeInstance {

    protected final ScreenShakeConfig config;
    private final long seedYaw;
    private final long seedPitch;
    private final long seedRoll;
    private int age;

    protected ScreenShakeInstance(ScreenShakeConfig config, long baseSeed) {
        this.config    = config;
        this.seedYaw   = baseSeed;
        this.seedPitch = baseSeed + 100_003L;
        this.seedRoll  = baseSeed + 200_003L;
        this.age       = 0;
    }

    public final void tick() {
        if (!isDead()) {
            age++;
        }
    }

    public final boolean isDead() {
        return age >= config.totalTicks();
    }

    protected final float envelope(float partialTick) {
        float t = age + partialTick;
        float total = config.totalTicks();
        if (total <= 0f) return 0f;

        float attackEnd  = config.attackTicks();
        float sustainEnd = attackEnd + config.sustainTicks();

        if (t <= attackEnd) {
            if (attackEnd <= 0f) return 1f;
            return cosineRamp(t / attackEnd);
        } else if (t <= sustainEnd) {
            return 1f;
        } else {
            float fadeoutLen = config.fadeoutTicks();
            if (fadeoutLen <= 0f) return 0f;
            float fadeProgress = (t - sustainEnd) / fadeoutLen;
            return cosineRamp(1f - Math.min(fadeProgress, 1f));
        }
    }

    private static float cosineRamp(float x) {
        return (1f - (float) Math.cos(x * Math.PI)) * 0.5f;
    }

    public final float[] getCurrentOffsets(Camera camera, float partialTick) {
        if (isDead()) return new float[]{0f, 0f, 0f};

        float timeSec = (age + partialTick) * 0.05f;
        float env = envelope(partialTick);
        float scale = env * intensityScale(camera);

        float yaw = SmoothNoise.fbm(timeSec, seedYaw, config.noiseLayers(),
                config.frequency(), config.lacunarity(), config.persistence())
                * config.maxYawDeg() * scale;

        float pitch = SmoothNoise.fbm(timeSec, seedPitch, config.noiseLayers(),
                config.frequency(), config.lacunarity(), config.persistence())
                * config.maxPitchDeg() * scale;

        float roll = SmoothNoise.fbm(timeSec, seedRoll, config.noiseLayers(),
                config.frequency(), config.lacunarity(), config.persistence())
                * config.maxRollDeg() * scale;

        return new float[]{yaw, pitch, roll};
    }

    protected float intensityScale(Camera camera) {
        return 1.0f;
    }

    public ScreenShakeConfig getConfig() { return config; }
    public int getAge()                  { return age; }

    public float getNormalizedAge() {
        int total = config.totalTicks();
        return total > 0 ? (float) age / total : 1f;
    }
}
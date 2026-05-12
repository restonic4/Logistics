package com.restonic4.logistics.experiment;

public final class SmoothNoise {

    private SmoothNoise() {}

    public static float fbm(
            float t,
            long seed,
            int layers,
            float frequency,
            float lacunarity,
            float persistence
    ) {
        float value     = 0f;
        float amplitude = 1f;
        float maxValue  = 0f;
        float freq      = frequency;

        for (int i = 0; i < layers; i++) {
            value    += valueNoise(t * freq, seed + i * 1_000_003L) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            freq      *= lacunarity;
        }

        return maxValue > 0f ? value / maxValue : 0f;
    }

    private static float valueNoise(float t, long seed) {
        int   i  = (int) Math.floor(t);
        float f  = t - i;
        float f2 = cosineSmooth(f);

        float v0 = hashToFloat(i,     seed);
        float v1 = hashToFloat(i + 1, seed);

        return v0 + (v1 - v0) * f2;
    }

    private static float cosineSmooth(float t) {
        return (1f - (float) Math.cos(t * Math.PI)) * 0.5f;
    }

    /**
     * Maps an integer lattice point + seed to a pseudo-random float in [-1, 1].
     *
     * <p>Uses 24 bits of the hashed long. The previous implementation erroneously
     * multiplied by 2 after dividing by 2^23, yielding values up to 3.0.
     */
    private static float hashToFloat(int point, long seed) {
        long h = seed ^ (long) point;
        h ^= h >>> 30;
        h *= 0xbf58476d1ce4e5b9L;
        h ^= h >>> 27;
        h *= 0x94d049bb133111ebL;
        h ^= h >>> 31;

        // 24-bit unsigned value in [0, 16777215] mapped to [-1, 1).
        return ((int)(h >>> 40) / (float)(1 << 23)) - 1f;
    }
}
package com.restonic4.logistics.experiment;

public record ScreenShakeConfig(

        /** Peak yaw (left/right) displacement in degrees. */
        float maxYawDeg,

        /** Peak pitch (up/down) displacement in degrees. */
        float maxPitchDeg,

        /**
         * Peak roll displacement in degrees.
         * A subtle non-zero value (0.1–0.3°) adds realism without feeling drunk.
         */
        float maxRollDeg,

        /**
         * Base oscillation frequency in Hz (cycles per second).
         * The actual frequency per noise layer is scaled by {@code lacunarity}.
         */
        float frequency,

        /**
         * Lacunarity — frequency multiplier per successive noise octave.
         * 2.0 is standard; higher values pack more detail into upper octaves.
         */
        float lacunarity,

        /**
         * Persistence — amplitude multiplier per successive noise octave.
         * 0.5 means each octave contributes half the amplitude of the previous one.
         */
        float persistence,

        /** Number of noise octaves to stack for the yaw/pitch/roll noise. */
        int noiseLayers,

        /**
         * Ticks (1 tick = 50 ms) from spawn until the shake reaches full intensity.
         * 0 means instant onset.  Useful for softening the very start of long rumbles.
         */
        int attackTicks,

        /**
         * Ticks from peak intensity until the shake fully dies out.
         * This is the duration of the fade-out tail.
         */
        int fadeoutTicks,

        /**
         * Total active ticks EXCLUDING the fadeout.
         * The shake sustains full intensity for this long, then begins fading.
         * Set to 0 for an impact that only has an attack + immediate fade.
         */
        int sustainTicks

) {

    // -------------------------------------------------------------------------
    // Preset factory methods — expand freely, these are just starting points.
    // -------------------------------------------------------------------------

    /**
     * Small, snappy hit — punches, small projectile impacts.
     * Fast onset, short duration, mid frequency.
     */
    public static ScreenShakeConfig impact() {
        return new ScreenShakeConfig(
                0.6f,   // maxYawDeg
                0.4f,   // maxPitchDeg
                0.15f,  // maxRollDeg
                8.0f,   // frequency
                2.0f,   // lacunarity
                0.5f,   // persistence
                3,      // noiseLayers
                0,      // attackTicks  (instant)
                12,     // fadeoutTicks (~600 ms)
                0       // sustainTicks
        );
    }

    /**
     * Medium explosion — TNT, grenade, mid-range blast.
     */
    public static ScreenShakeConfig explosion() {
        return new ScreenShakeConfig(
                1.4f,   // maxYawDeg
                1.0f,   // maxPitchDeg
                0.3f,   // maxRollDeg
                5.0f,   // frequency
                2.0f,   // lacunarity
                0.5f,   // persistence
                3,      // noiseLayers
                2,      // attackTicks  (very fast onset)
                30,     // fadeoutTicks (~1.5 s)
                4       // sustainTicks (~200 ms at peak)
        );
    }

    /**
     * Large explosion — massive blast, close range.
     */
    public static ScreenShakeConfig bigExplosion() {
        return new ScreenShakeConfig(
                2.5f,   // maxYawDeg
                1.8f,   // maxPitchDeg
                0.5f,   // maxRollDeg
                3.5f,   // frequency  (lower = more lumbering)
                2.0f,   // lacunarity
                0.45f,  // persistence
                4,      // noiseLayers
                3,      // attackTicks
                60,     // fadeoutTicks (~3 s)
                10      // sustainTicks
        );
    }

    /**
     * Continuous ground rumble — earthquake, large machinery nearby.
     * Intended for repeated or looping spawns rather than a single shot.
     */
    public static ScreenShakeConfig rumble() {
        return new ScreenShakeConfig(
                0.5f,   // maxYawDeg
                0.3f,   // maxPitchDeg
                0.1f,   // maxRollDeg
                2.0f,   // frequency
                2.0f,   // lacunarity
                0.6f,   // persistence
                2,      // noiseLayers
                10,     // attackTicks (slow build)
                40,     // fadeoutTicks
                40      // sustainTicks (long hold)
        );
    }

    public static ScreenShakeConfig shockwave() {
        return new ScreenShakeConfig(
                2.0f,   // maxYawDeg      — strong horizontal kick
                1.2f,   // maxPitchDeg    — sharp vertical jolt
                0.4f,   // maxRollDeg     — subtle barrel distortion
                12.0f,  // frequency      — high-frequency snap (impact frame)
                2.0f,   // lacunarity     — standard octave spread
                0.4f,   // persistence    — lower = sharper attack, cleaner tail
                4,      // noiseLayers    — 4 octaves for crisp detail without mush
                1,      // attackTicks    — near-instant onset (1 tick = 50ms)
                80,     // fadeoutTicks   — ~1.25s tail, enough to feel the pressure wave
                15       // sustainTicks   — 3 ticks (~150ms) at peak before decay
        );
    }

    /** Total lifetime in ticks: attack + sustain + fadeout. */
    public int totalTicks() {
        return attackTicks + sustainTicks + fadeoutTicks;
    }
}
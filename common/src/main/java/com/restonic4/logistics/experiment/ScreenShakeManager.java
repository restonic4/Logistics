package com.restonic4.logistics.experiment;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-side singleton that manages all active {@link ScreenShakeInstance}s.
 */
public final class ScreenShakeManager {

    private static final ScreenShakeManager INSTANCE = new ScreenShakeManager();

    public static ScreenShakeManager getInstance() {
        return INSTANCE;
    }

    private ScreenShakeManager() {}

    public static float MAX_ACCUMULATED_YAW_DEG   = 4.0f;
    public static float MAX_ACCUMULATED_PITCH_DEG = 3.0f;
    public static float MAX_ACCUMULATED_ROLL_DEG  = 1.5f;
    public static float GLOBAL_INTENSITY = 1.0f;

    private static final int MAX_ACTIVE_SHAKES = 128;

    private final List<ScreenShakeInstance> activeShakes = new CopyOnWriteArrayList<>();
    private long nextSeed = 0L;

    public GlobalScreenShake spawnGlobal(ScreenShakeConfig config) {
        GlobalScreenShake shake = new GlobalScreenShake(config, nextSeed());
        add(shake);
        return shake;
    }

    public PositionedScreenShake spawnPositioned(
            ScreenShakeConfig config,
            Vec3 origin,
            double innerRadius,
            double maxRadius,
            PositionedScreenShake.FalloffCurve curve
    ) {
        PositionedScreenShake shake = new PositionedScreenShake(
                config, nextSeed(), origin, innerRadius, maxRadius, curve
        );
        add(shake);
        return shake;
    }

    public PositionedScreenShake spawnPositioned(
            ScreenShakeConfig config, Vec3 origin, double maxRadius
    ) {
        return spawnPositioned(config, origin, 0.0, maxRadius,
                PositionedScreenShake.FalloffCurve.SMOOTH_STEP);
    }

    public void tick() {
        for (ScreenShakeInstance shake : activeShakes) {
            shake.tick();
        }
        activeShakes.removeIf(ScreenShakeInstance::isDead);
    }

    public float[] getAccumulatedOffsets(Camera camera, float partialTick) {
        if (GLOBAL_INTENSITY <= 0f || activeShakes.isEmpty()) {
            return new float[]{0f, 0f, 0f};
        }

        float totalYaw = 0f, totalPitch = 0f, totalRoll = 0f;

        for (ScreenShakeInstance shake : activeShakes) {
            float[] offsets = shake.getCurrentOffsets(camera, partialTick);
            totalYaw   += offsets[0];
            totalPitch += offsets[1];
            totalRoll  += offsets[2];
        }

        totalYaw   *= GLOBAL_INTENSITY;
        totalPitch *= GLOBAL_INTENSITY;
        totalRoll  *= GLOBAL_INTENSITY;

        totalYaw   = softClamp(totalYaw,   MAX_ACCUMULATED_YAW_DEG);
        totalPitch = softClamp(totalPitch, MAX_ACCUMULATED_PITCH_DEG);
        totalRoll  = softClamp(totalRoll,  MAX_ACCUMULATED_ROLL_DEG);

        return new float[]{totalYaw, totalPitch, totalRoll};
    }

    public void clearAll() {
        activeShakes.clear();
    }

    public int getActiveCount() {
        return activeShakes.size();
    }

    private void add(ScreenShakeInstance shake) {
        if (activeShakes.size() >= MAX_ACTIVE_SHAKES) {
            activeShakes.remove(0);
        }
        activeShakes.add(shake);
    }

    private long nextSeed() {
        return nextSeed++;
    }

    /**
     * Rational soft-clamp: linear inside [-limit, limit], then compresses
     * asymptotically toward ±limit with the curve x/(1+|x|). Smooth, cheap,
     * and avoids the hard discontinuity of a naive min/max clamp.
     */
    private static float softClamp(float value, float limit) {
        if (limit <= 0f) return 0f;
        float normalized = value / limit;
        float saturated = normalized / (1f + Math.abs(normalized));
        return saturated * limit;
    }
}
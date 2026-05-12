package com.restonic4.logistics.experiment;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

public final class PositionedScreenShake extends ScreenShakeInstance {

    public enum FalloffCurve {
        LINEAR {
            @Override
            public float apply(float t) { return t; }
        },
        QUADRATIC {
            @Override
            public float apply(float t) { return t * t; }
        },
        SMOOTH_STEP {
            @Override
            public float apply(float t) { return t * t * (3f - 2f * t); }
        },
        SMOOTHER_STEP {
            @Override
            public float apply(float t) {
                return t * t * t * (t * (t * 6f - 15f) + 10f);
            }
        };

        public abstract float apply(float t);
    }

    private final Vec3 origin;
    private final double innerRadius;
    private final double maxRadius;
    private final FalloffCurve falloffCurve;

    public PositionedScreenShake(
            ScreenShakeConfig config,
            long baseSeed,
            Vec3 origin,
            double innerRadius,
            double maxRadius,
            FalloffCurve falloffCurve
    ) {
        super(config, baseSeed);
        if (maxRadius <= innerRadius) {
            throw new IllegalArgumentException(
                    "maxRadius (" + maxRadius + ") must be > innerRadius (" + innerRadius + ")"
            );
        }
        this.origin       = origin;
        this.innerRadius  = innerRadius;
        this.maxRadius    = maxRadius;
        this.falloffCurve = falloffCurve;
    }

    public PositionedScreenShake(
            ScreenShakeConfig config, long baseSeed, Vec3 origin, double maxRadius
    ) {
        this(config, baseSeed, origin, 0.0, maxRadius, FalloffCurve.SMOOTH_STEP);
    }

    @Override
    protected float intensityScale(Camera camera) {
        Vec3 camPos = camera.getPosition();
        double dist = camPos.distanceTo(origin);

        if (dist <= innerRadius) return 1.0f;
        if (dist >= maxRadius)   return 0.0f;

        double t = (dist - innerRadius) / (maxRadius - innerRadius);
        return 1.0f - falloffCurve.apply((float) t);
    }

    public Vec3 getOrigin()               { return origin; }
    public double getInnerRadius()        { return innerRadius; }
    public double getMaxRadius()          { return maxRadius; }
    public FalloffCurve getFalloffCurve() { return falloffCurve; }
}
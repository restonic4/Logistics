package com.restonic4.logistics.experiment;

import net.minecraft.client.Camera;

public final class GlobalScreenShake extends ScreenShakeInstance {

    public GlobalScreenShake(ScreenShakeConfig config, long baseSeed) {
        super(config, baseSeed);
    }

    @Override
    protected float intensityScale(Camera camera) {
        return 1.0f;
    }
}
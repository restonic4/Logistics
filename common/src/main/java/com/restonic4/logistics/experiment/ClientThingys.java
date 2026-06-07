package com.restonic4.logistics.experiment;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;

public class ClientThingys {
    private static final float MIN_EFFECT_DISTANCE = 16.0f;
    private static final float MAX_EFFECT_DISTANCE = 320.0f;
    private static final float MIN_VOLUME = 0.15f;
    private static final float MAX_VOLUME = 0.75f;

    public static void shockwave(BlockPos origin, double maxRadius, double thickness, double expansionDuration, double fadeOutDuration, int colorARGB) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            double distance = player.position().distanceTo(origin.getCenter());
            if (distance <= maxRadius * 2.0) {
                ShockwaveInstance instance = ShockwaveManager.createPreloaded(origin, maxRadius, thickness, expansionDuration, fadeOutDuration, colorARGB);

                if (instance != null) {
                    instance.setOnLoadCallback(timeMs -> {
                        instance.activate();

                        if (maxRadius < MIN_EFFECT_DISTANCE) {
                            return;
                        }

                        float volume;
                        if (maxRadius >= MAX_EFFECT_DISTANCE) {
                            volume = MAX_VOLUME;
                        } else {
                            float t = (float) ((maxRadius - MIN_EFFECT_DISTANCE) / (MAX_EFFECT_DISTANCE - MIN_EFFECT_DISTANCE));
                            volume = MIN_VOLUME + t * (MAX_VOLUME - MIN_VOLUME);
                        }

                        Minecraft.getInstance().getSoundManager().play(
                                SimpleSoundInstance.forUI(
                                        Sounds.SHOCKWAVE.getSoundEvent(),
                                        1.0f,
                                        volume
                                )
                        );

                        float shakeIntensity = (float) Math.min(maxRadius / MAX_EFFECT_DISTANCE, 1.0);
                        ScreenShakeConfig base = ScreenShakeConfig.shockwave();
                        ScreenShakeConfig scaled = new ScreenShakeConfig(
                                base.maxYawDeg() * shakeIntensity,
                                base.maxPitchDeg() * shakeIntensity,
                                base.maxRollDeg() * shakeIntensity,
                                base.frequency(),
                                base.lacunarity(),
                                base.persistence(),
                                base.noiseLayers(),
                                base.attackTicks(),
                                base.fadeoutTicks(),
                                base.sustainTicks()
                        );

                        ScreenShakeManager.getInstance().spawnPositioned(scaled, origin.getCenter(), maxRadius);
                    });
                }
            }
        }
    }
}

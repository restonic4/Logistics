package com.restonic4.logistics.experiment;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;

public class ClientThingys {
    public static void shockwave(BlockPos origin, double maxRadius, double thickness, double expansionDuration, double fadeOutDuration, int colorARGB) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            double distance = player.position().distanceTo(origin.getCenter());
            if (distance <= maxRadius * 2.0) {
                ShockwaveInstance instance = ShockwaveManager.createPreloaded(
                        origin, maxRadius, thickness, expansionDuration, fadeOutDuration, colorARGB
                );

                if (instance != null) {
                    instance.setOnLoadCallback(timeMs -> {
                        instance.activate();

                        Minecraft.getInstance().getSoundManager().play(
                                SimpleSoundInstance.forUI(
                                        Sounds.SHOCKWAVE.getSoundEvent(),
                                        1.0f,
                                        1.0f
                                )
                        );

                        ScreenShakeManager.getInstance().spawnPositioned(ScreenShakeConfig.shockwave(), origin.getCenter(), maxRadius);
                    });
                }
            }
        }
    }
}

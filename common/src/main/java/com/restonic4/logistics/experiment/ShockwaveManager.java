package com.restonic4.logistics.experiment;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ShockwaveManager {
    private static final List<ShockwaveInstance> ACTIVE_SHOCKWAVES = new ArrayList<>();
    private static final double DEFAULT_INTENSITY_MS = 1.5;

    public static void clientTick(ClientLevel level) {
        if (ACTIVE_SHOCKWAVES.isEmpty()) return;

        Iterator<ShockwaveInstance> it = ACTIVE_SHOCKWAVES.iterator();
        while (it.hasNext()) {
            ShockwaveInstance instance = it.next();

            // Cleaned up tracking and structural bugs. Lifecycle checks are asynchronous now.
            if (instance.isExpired()) {
                it.remove();
            }
        }
    }

    public static void spawn(BlockPos origin, double maxRadius, double thickness, double expansionDuration, double fadeOutDuration, int colorARGB) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        ShockwaveInstance instance = new ShockwaveInstance(
                level, origin, maxRadius, thickness, expansionDuration, fadeOutDuration, colorARGB, DEFAULT_INTENSITY_MS
        );
        instance.activate();
        ACTIVE_SHOCKWAVES.add(instance);
    }

    public static ShockwaveInstance createPreloaded(BlockPos origin, double maxRadius, double thickness, double expansionDuration, double fadeOutDuration, int colorARGB) {
        return createPreloaded(origin, maxRadius, thickness, expansionDuration, fadeOutDuration, colorARGB, DEFAULT_INTENSITY_MS);
    }

    public static ShockwaveInstance createPreloaded(BlockPos origin, double maxRadius, double thickness, double expansionDuration, double fadeOutDuration, int colorARGB, double scanIntensityMs) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;

        ShockwaveInstance instance = new ShockwaveInstance(
                level, origin, maxRadius, thickness, expansionDuration, fadeOutDuration, colorARGB, scanIntensityMs
        );
        ACTIVE_SHOCKWAVES.add(instance);
        return instance;
    }

    public static void renderAll(PoseStack poseStack, double camX, double camY, double camZ, Frustum frustum) {
        if (ACTIVE_SHOCKWAVES.isEmpty()) return;

        for (int i = 0; i < ACTIVE_SHOCKWAVES.size(); i++) {
            ACTIVE_SHOCKWAVES.get(i).tickAndRender(poseStack, camX, camY, camZ, frustum);
        }
    }
}
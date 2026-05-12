package com.restonic4.logistics.mixin.experiment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.restonic4.logistics.experiment.ScreenShakeManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link GameRenderer} to apply accumulated screen shake offsets
 * to the camera rotation every rendered frame.
 *
 * <p>We hook {@code bobView} because it is the last view-space transform
 * applied before the scene is drawn. Modifying the {@link PoseStack} here
 * means ray-casting, hit-testing and fog (which all use the unmodified
 * {@link Camera} object) remain correct.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Final
    @Shadow
    private Camera mainCamera;

    @Inject(
            method = "bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
            at = @At("HEAD")
    )
    private void screenshake$applyShake(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        ScreenShakeManager manager = ScreenShakeManager.getInstance();

        if (manager.getActiveCount() == 0) {
            return;
        }

        float[] offsets = manager.getAccumulatedOffsets(mainCamera, partialTick);
        float yawDeg   = offsets[0];
        float pitchDeg = offsets[1];
        float rollDeg  = offsets[2];

        if (Math.abs(yawDeg) < 1e-4f
                && Math.abs(pitchDeg) < 1e-4f
                && Math.abs(rollDeg) < 1e-4f) {
            return;
        }

        // Rotation order: yaw (Y), pitch (X), roll (Z) — matches Minecraft convention.
        poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchDeg));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rollDeg));
    }
}
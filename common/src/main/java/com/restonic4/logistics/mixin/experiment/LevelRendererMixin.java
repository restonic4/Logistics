package com.restonic4.logistics.mixin.experiment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.restonic4.logistics.experiment.ShockwaveManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    private Frustum cullingFrustum;

    @Inject(method = "renderChunkLayer", at = @At("HEAD"))
    private void onRenderChunkLayer(
            RenderType renderType,
            PoseStack poseStack,
            double camX,
            double camY,
            double camZ,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        if (renderType == RenderType.translucent()) {
            ShockwaveManager.renderAll(poseStack, camX, camY, camZ, this.cullingFrustum);
        }
    }
}
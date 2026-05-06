package com.restonic4.logistics.mixin.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.restonic4.logistics.events.RenderCallbacks;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(
            method = "renderLevel",
            at = @At("TAIL")
    )
    private void onRenderLevelLast(
            PoseStack poseStack,
            float partialTick,
            long finishNanoTime,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        poseStack.pushPose();
        RenderCallbacks.ON_LEVEL_RENDERED.invoker().onEvent(poseStack, camera);
        poseStack.popPose();
    }
}

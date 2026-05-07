package com.restonic4.logistics.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.utils.MathHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.*;

public class RenderingHelper {
    public static final LabelBatcher LABEL_BATCHER = new LabelBatcher();

    public static void rawLabel(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, double x, double y, double z, int packedColor, double maxDistance, String text) {
        Vec3 camPos = camera.getPosition();
        Font font = Minecraft.getInstance().font;

        double dx = x - camPos.x;
        double dy = y - camPos.y;
        double dz = z - camPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq > maxDistance * maxDistance) return;

        poseStack.pushPose();
        poseStack.translate(dx, dy + 1.2, dz);
        poseStack.mulPose(camera.rotation());

        float scale = (float) Math.sqrt(distSq) * 0.0025f;
        scale = Math.max(scale, 0.025f);
        poseStack.scale(-scale, -scale, scale);

        float textWidth = font.width(text);
        float renderX = -textWidth / 2f;

        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(text, renderX, 0, packedColor, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        font.drawInBatch(text, renderX, 0, packedColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);

        poseStack.popPose();
    }

    public static void rawLabel(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, Vec3 position, int packedColor, double maxDistance, String text) {
        rawLabel(bufferSource, poseStack, camera, position.x, position.y, position.z, packedColor, maxDistance, text);
    }

    public static void label(PoseStack poseStack, Camera camera, Vec3 position, int packedColor, double maxDistance, String text) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        rawLabel(bufferSource, poseStack, camera, position, packedColor, maxDistance, text);
        bufferSource.endBatch();
    }

    public static class LabelBatcher {
        private MultiBufferSource.BufferSource bufferSource;
        private PoseStack poseStack;
        private Camera camera;
        private double maxDistance;

        public void begin(PoseStack poseStack, Camera camera, double maxDistance) {
            this.bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            this.poseStack = poseStack;
            this.camera = camera;
            this.maxDistance = maxDistance;
        }

        public void draw(Vec3 position, int packedColor, String text) {
            rawLabel(this.bufferSource, this.poseStack, this.camera, position, packedColor, this.maxDistance, text);
        }

        public void draw(double x, double y, double z, int packedColor, String text) {
            rawLabel(this.bufferSource, this.poseStack, this.camera, x, y, z, packedColor, this.maxDistance, text);
        }

        public void end() {
            if (this.bufferSource != null) {
                this.bufferSource.endBatch();
            }

            this.bufferSource = null;
            this.poseStack = null;
            this.camera = null;
        }
    }
}

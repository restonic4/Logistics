package com.restonic4.logistics.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.restonic4.logistics.events.RenderCallbacks;
import com.restonic4.logistics.networks.pathfinding.Parcel;
import com.restonic4.logistics.networks.pathfinding.ParcelRenderSyncPacket;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class ParcelRenderer {
    private static List<Parcel> parcels = new ArrayList<>();

    public static void setParcels(ParcelRenderSyncPacket payload) {
        parcels = payload.parcels();
    }

    public static void register() {
        RenderCallbacks.ON_LEVEL_RENDERED.register((poseStack, camera) -> {
            if (parcels.isEmpty()) return;

            Minecraft mc = Minecraft.getInstance();
            ItemRenderer itemRenderer = mc.getItemRenderer();
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

            for (Parcel parcel : parcels) {
                render(poseStack, camera, bufferSource, itemRenderer, parcel);
            }

            bufferSource.endBatch();
        });
    }

    public static void render(
            PoseStack poseStack,
            Camera camera,
            MultiBufferSource.BufferSource bufferSource,
            ItemRenderer itemRenderer,
            Parcel parcel
    ) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 camPos = camera.getPosition();
        Vec3 pos = parcel.getPosition();
        ItemStack stack = parcel.getItemStackClone();

        int count = stack.getCount();
        int renderCount = count <= 4 ? 1 : count <= 16 ? 2 : count <= 32 ? 3 : 4;

        BakedModel model = itemRenderer.getModel(stack, null, null, 0);

        float age = (mc.level.getGameTime() + mc.getFrameTime()) / 20.0f;
        float rotation = age * (360.0f / 2.0f) % 360.0f;

        float radius = renderCount == 1 ? 0.0f : 0.07f;

        for (int i = 0; i < renderCount; i++) {
            poseStack.pushPose();

            poseStack.translate(
                    pos.x - camPos.x + 0.5,
                    pos.y - camPos.y + 0.5 - 0.1,
                    pos.z - camPos.z + 0.5
            );

            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

            if (renderCount > 1) {
                double angle = (2.0 * Math.PI / renderCount) * i;
                float offsetX = (float) Math.cos(angle) * radius;
                float offsetZ = (float) Math.sin(angle) * radius;
                float offsetY = (i % 2 == 0 ? 0.03f : -0.03f);
                poseStack.translate(offsetX, offsetY, offsetZ);
            }

            float scale = 0.75f;
            poseStack.scale(scale, scale, scale);

            itemRenderer.render(
                    stack,
                    ItemDisplayContext.GROUND,
                    false,
                    poseStack,
                    bufferSource,
                    15728880,
                    OverlayTexture.NO_OVERLAY,
                    model
            );

            poseStack.popPose();
        }
    }
}

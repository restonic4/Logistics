package com.restonic4.logistics.blocks.charging_station;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ChargingStationRenderer implements BlockEntityRenderer<ChargingStationBlockEntity> {
    private final ItemRenderer itemRenderer;

    public ChargingStationRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ChargingStationBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        ItemStack stack = blockEntity.getRenderStack();
        if (stack.isEmpty()) return;

        poseStack.pushPose();

        poseStack.translate(0.5f, 0.9f, 0.5f);
        poseStack.scale(0.75f, 0.75f, 0.75f);

        if (blockEntity.getLevel() != null) {
            float gameTime = blockEntity.getLevel().getGameTime() + partialTick;
            poseStack.mulPose(Axis.YP.rotationDegrees(gameTime * 2.5f));
        }

        this.itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                combinedLight,
                combinedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                (int) blockEntity.getBlockPos().asLong()
        );

        poseStack.popPose();
    }
}
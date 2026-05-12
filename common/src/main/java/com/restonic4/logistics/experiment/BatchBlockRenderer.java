package com.restonic4.logistics.experiment;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;

public class BatchBlockRenderer {
    private float r, g, b, a;
    private final float sizeOffset = 0.005f; // Defeats Z-fighting against world geometry

    public BatchBlockRenderer(int colorARGB) {
        setColor(colorARGB);
    }

    public void setColor(int colorARGB) {
        this.a = ((colorARGB >> 24) & 0xFF) / 255.0f;
        this.r = ((colorARGB >> 16) & 0xFF) / 255.0f;
        this.g = ((colorARGB >> 8) & 0xFF) / 255.0f;
        this.b = (colorARGB & 0xFF) / 255.0f;
        if (this.a == 0.0f) this.a = 1.0f; // Safe fallback if caller omitted alpha
    }

    /**
     * Renders a list of blocks using their precomputed visibility masks.
     * No HashSet lookups, no neighbor queries, no intra-shockwave culling.
     * Everything is pure vertex emission.
     */
    public void render(PoseStack poseStack, double camX, double camY, double camZ,
                       LongArrayList positions, ByteArrayList masks) {
        int totalBlocks = positions.size();
        if (totalBlocks == 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        for (int i = 0; i < totalBlocks; i++) {
            long packed = positions.getLong(i);
            int x = BlockPos.getX(packed);
            int y = BlockPos.getY(packed);
            int z = BlockPos.getZ(packed);
            byte mask = masks.getByte(i);
            if (mask == 0) continue;

            float minX = (float) (x - camX) - sizeOffset;
            float maxX = (float) (x + 1 - camX) + sizeOffset;
            float minY = (float) (y - camY) - sizeOffset;
            float maxY = (float) (y + 1 - camY) + sizeOffset;
            float minZ = (float) (z - camZ) - sizeOffset;
            float maxZ = (float) (z + 1 - camZ) + sizeOffset;

            // Camera-sided face culling: cheap early reject before vertex emission.
            // This is conservative (orthographic-like) but saves enormous CPU work
            // and pairs well with the Frustum culling already done in ShockwaveInstance.
            if ((mask & 0x01) != 0 && camY < y) { // DOWN
                builder.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();
            }
            if ((mask & 0x02) != 0 && camY > y + 1) { // UP
                builder.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();
            }
            if ((mask & 0x04) != 0 && camZ < z) { // NORTH
                builder.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();
            }
            if ((mask & 0x08) != 0 && camZ > z + 1) { // SOUTH
                builder.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();
            }
            if ((mask & 0x10) != 0 && camX < x) { // WEST
                builder.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();
            }
            if ((mask & 0x20) != 0 && camX > x + 1) { // EAST
                builder.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
                builder.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
            }
        }

        BufferUploader.drawWithShader(builder.end());
    }
}
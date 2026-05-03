package com.restonic4.logistics.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.utils.MathHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.*;

public final class NetworkDebugRenderer {
    private static final double LABEL_MAX_DIST = 64.0;

    private static final float CUBE_ALPHA = 0.35f;
    private static final float WIRE_ALPHA = 0.80f;
    private static final float CUBE_EXPAND = 0.02f;

    private static Minecraft minecraft = Minecraft.getInstance();

    public static void render(PoseStack poseStack, Camera camera) {
        if (minecraft.level == null || minecraft.player == null) return;
        if (minecraft.getSingleplayerServer() == null) return;

        ServerLevel serverLevel = minecraft.getSingleplayerServer().getLevel(minecraft.level.dimension());
        if (serverLevel == null) return;

        NetworkManager manager = NetworkManager.get(serverLevel);
        Collection<Network> energyNetworks = manager.getAllNetworks();
        try {
            energyNetworks = new ArrayList<>(energyNetworks.stream().toList());
        } catch (ConcurrentModificationException e) {
            energyNetworks = Collections.emptyList();
        }

        if (energyNetworks.isEmpty()) return;

        Vec3 camPos = camera.getPosition();
        Map<UUID, float[]> networkTints = buildNetworkTints(energyNetworks);

        // Bind the correct shader before drawing shapes
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        // Extract the transformation matrix to apply pitch and yaw to the vertices
        Matrix4f matrix = poseStack.last().pose();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buf = tessellator.getBuilder();

        // ── Pass 1: draw filled transparent cubes ────────────────────────────
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Network energyNetwork : energyNetworks) {
            float[] tint = networkTints.get(energyNetwork.getUUID());

            List<NetworkNode> memberPositions;
            try {
                memberPositions = energyNetwork.getNodeIndex().getAllNodes().stream().toList();
                memberPositions = new ArrayList<>(memberPositions.stream().toList());
            } catch (Exception ignored) {
                memberPositions = Collections.emptyList();
            }

            for (NetworkNode node : memberPositions) {
                GeometryHelper.filledCube(
                        buf, matrix, node.getBlockPos(), camPos,
                        tint[0] * 0.4f, tint[1] * 0.4f, tint[2] * 0.4f, CUBE_ALPHA * 0.5f,
                        CUBE_EXPAND
                );
            }
        }
        tessellator.end();

        // ── Pass 2: draw wireframe outlines ──────────────────────────────────
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        for (Network energyNetwork : energyNetworks) {
            float[] tint = networkTints.get(energyNetwork.getUUID());

            List<NetworkNode> memberPositions;
            try {
                memberPositions = energyNetwork.getNodeIndex().getAllNodes().stream().toList();
                memberPositions = new ArrayList<>(memberPositions.stream().toList());
            } catch (Exception ignored) {
                memberPositions = Collections.emptyList();
            }

            for (NetworkNode node : memberPositions) {
                GeometryHelper.wireframeCube(
                        buf, matrix, node.getBlockPos(), camPos,
                        tint[0] * 0.5f, tint[1] * 0.5f, tint[2] * 0.5f, WIRE_ALPHA * 0.4f,
                        CUBE_EXPAND
                );
            }
        }
        tessellator.end();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        // ── Pass 3: draw floating labels ─────────────────────────────────────
        renderLabels(poseStack, camera, energyNetworks, networkTints);
    }

    private static void renderLabels(PoseStack poseStack, Camera camera, Collection<Network> energyNetworks, Map<UUID, float[]> tints) {
        Vec3 camPos = camera.getPosition();
        Font font = Minecraft.getInstance().font;
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        for (Network energyNetwork : energyNetworks) {
            List<NetworkNode> memberPositions;
            try {
                memberPositions = energyNetwork.getNodeIndex().getAllNodes().stream().toList();
                memberPositions = new ArrayList<>(memberPositions.stream().toList());
            } catch (Exception ignored) {
                memberPositions = Collections.emptyList();
            }

            // Get the block closest to the camera instead of the network center
            Vec3 targetPos = MathHelper.closestTo(MathHelper.toVec3(new HashSet<>(memberPositions)), camPos);
            if (targetPos == null) continue;

            double distSq = targetPos.distanceToSqr(camPos);
            if (distSq > LABEL_MAX_DIST * LABEL_MAX_DIST) continue;

            float[] t = tints.get(energyNetwork.getUUID());
            int color = MathHelper.packColor(t[0], t[1], t[2], 1.0f);

            poseStack.pushPose();

            // Translate to the closest block, lifted 1.2 blocks above its center
            poseStack.translate(targetPos.x - camPos.x, targetPos.y - camPos.y + 1.2, targetPos.z - camPos.z);

            // Billboard: face the camera
            poseStack.mulPose(camera.rotation());

            // Scale based on distance so it doesn't get unreadably tiny
            float scale = (float) Math.sqrt(distSq) * 0.0025f;
            scale = Math.max(scale, 0.025f);
            poseStack.scale(-scale, -scale, scale);

            String text = String.format("Net %s | %s | %d members",
                    energyNetwork.getUUID().toString().substring(0, 4),
                    energyNetwork.getResourceLocation(),
                    energyNetwork.getNodeIndex().getAllNodes().size());

            float textWidth = font.width(text);
            float x = -textWidth / 2f;

            Matrix4f matrix = poseStack.last().pose();
            font.drawInBatch(text, x, 0, color, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
            font.drawInBatch(text, x, 0, color, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);

            poseStack.popPose();
        }
        bufferSource.endBatch();
    }

    private static Map<UUID, float[]> buildNetworkTints(Collection<Network> energyNetworks) {
        Map<UUID, float[]> map = new HashMap<>();
        for (Network net : energyNetworks) {
            long bits = net.getUUID().getLeastSignificantBits();
            float hue = (float) ((bits & 0xFFFFFFFFL) / (double) 0x100000000L);
            map.put(net.getUUID(), MathHelper.hsvToRgb(hue, 0.85f, 1.0f));
        }
        return map;
    }
}
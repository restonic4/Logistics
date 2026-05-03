package com.restonic4.logistics;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.restonic4.logistics.networks.energy.Network;
import com.restonic4.logistics.networks.energy.NetworkManager;
import com.restonic4.logistics.networks.energy.NetworkNode;
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

/**
 * Debug renderer that draws colored cubes over every network member and
 * floating nametag-style labels at each network's centroid.
 *
 * <p><b>Enable / disable:</b> flip {@link #ENABLED} at compile time (or wire
 * it to a keybind at runtime via {@link #toggle()}).  When {@code false} the
 * renderer is completely silent — no packets, no CPU work.
 *
 * <p><b>Op-gate:</b> {@link #isAllowed()} returns {@code false} unless the
 * current session is a singleplayer integrated server <em>and</em> the local
 * player has operator permissions (permission level ≥ 2).  Wire this check
 * wherever you call {@link #render} (e.g. your {@code RenderLevelStageEvent}
 * handler) so non-ops never even trigger the path.
 *
 * <p><b>Registration example</b> (Fabric, in your ClientModInitializer):
 * <pre>{@code
 * WorldRenderEvents.LAST.register(context -> {
 *     if (!EnergyNetworkDebugRenderer.isAllowed()) return;
 *     EnergyNetworkDebugRenderer.render(
 *         context.matrixStack(),
 *         context.camera()
 *     );
 * });
 * }</pre>
 */
public final class EnergyNetworkDebugRenderer {

    // -------------------------------------------------------------------------
    // Master switch — set to false for production builds.
    // -------------------------------------------------------------------------

    /**
     * Compile-time constant.  When {@code false} every public method in this
     * class becomes a no-op so there is zero overhead in production builds.
     */
    public static final boolean ENABLED = true;

    // -------------------------------------------------------------------------
    // Runtime toggle (only meaningful when ENABLED == true)
    // -------------------------------------------------------------------------

    private static boolean visible = true;

    private static final double LABEL_MAX_DIST = 64.0;

    public static void toggle() {
        if (!ENABLED) return;
        visible = !visible;
    }

    public static boolean isVisible() {
        return ENABLED && visible;
    }

    // -------------------------------------------------------------------------
    // Op gate
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} only when the local player is an operator
     * (permission level ≥ 2) on an integrated server.  On a dedicated server
     * we never have server-side manager access, so we always return false.
     */
    public static boolean isAllowed() {
        if (!ENABLED) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() == null) return false;
        if (mc.player == null) return false;
        // Permission level 2 = "game master" / standard op level
        return mc.player.hasPermissions(2);
    }

    // -------------------------------------------------------------------------
    // Cube colors per node role  (ARGB packed, alpha intentionally low)
    // -------------------------------------------------------------------------

    private static final float CUBE_ALPHA  = 0.35f;
    private static final float WIRE_ALPHA  = 0.80f;
    private static final float CUBE_EXPAND = 0.02f; // slight grow so wires show on block surface

    // Each entry: { r, g, b }
    private static final float[] COLOR_PRODUCER = { 1.00f, 0.80f, 0.10f }; // amber
    private static final float[] COLOR_CONSUMER = { 0.20f, 0.60f, 1.00f }; // sky blue
    private static final float[] COLOR_STORAGE  = { 0.40f, 1.00f, 0.40f }; // lime
    private static final float[] COLOR_PIPE     = { 0.70f, 0.70f, 0.70f }; // grey
    private static final float[] COLOR_UNKNOWN  = { 1.00f, 0.30f, 1.00f }; // magenta

    // Network label background (RGBA)
    private static final int LABEL_BG_COLOR = 0x99000000; // semi-transparent black

    // -------------------------------------------------------------------------
    // Public render entry point
    // -------------------------------------------------------------------------

    /**
     * Call this from a {@code WorldRenderEvents.LAST} (Fabric) or equivalent
     * hook.  Guard with {@link #isAllowed()} before calling.
     *
     * @param poseStack the pose stack provided by the render event
     * @param camera    the current camera (used to compute the camera offset)
     */
    public static void render(PoseStack poseStack, Camera camera) {
        if (!ENABLED || !visible) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.getSingleplayerServer() == null) return;

        ServerLevel serverLevel = mc.getSingleplayerServer().getLevel(mc.level.dimension());
        if (serverLevel == null) return;

        NetworkManager manager = NetworkManager.get(serverLevel);
        Collection<Network> networks = manager.getAllNetworks();
        try {
            networks = new ArrayList<>(networks.stream().toList());
        } catch (ConcurrentModificationException e) {
            networks = Collections.emptyList();
        }

        if (networks.isEmpty()) return;

        Vec3 camPos = camera.getPosition();
        Map<UUID, float[]> networkTints = buildNetworkTints(networks);

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

        for (Network network : networks) {
            float[] tint = networkTints.get(network.getUUID());

            List<NetworkNode> memberPositions;
            try {
                memberPositions = network.getNodeIndex().getAllNodes().stream().toList();
                memberPositions = new ArrayList<>(memberPositions.stream().toList());
            } catch (Exception ignored) {
                memberPositions = Collections.emptyList();
            }

            for (NetworkNode node : memberPositions) {
                addFilledCube(buf, matrix, node.getBlockPos(), camPos,
                        tint[0] * 0.4f, tint[1] * 0.4f, tint[2] * 0.4f,
                        CUBE_ALPHA * 0.5f);
            }
        }
        tessellator.end();

        // ── Pass 2: draw wireframe outlines ──────────────────────────────────
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        for (Network network : networks) {
            float[] tint = networkTints.get(network.getUUID());

            List<NetworkNode> memberPositions;
            try {
                memberPositions = network.getNodeIndex().getAllNodes().stream().toList();
                memberPositions = new ArrayList<>(memberPositions.stream().toList());
            } catch (Exception ignored) {
                memberPositions = Collections.emptyList();
            }

            for (NetworkNode node : memberPositions) {
                addWireframeCube(buf, matrix, node.getBlockPos(), camPos,
                        tint[0] * 0.5f, tint[1] * 0.5f, tint[2] * 0.5f,
                        WIRE_ALPHA * 0.4f);
            }
        }
        tessellator.end();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        // ── Pass 3: draw floating labels ─────────────────────────────────────
        renderLabels(poseStack, camera, networks, networkTints);
    }

    // -------------------------------------------------------------------------
    // Label rendering
    // -------------------------------------------------------------------------

    private static void renderLabels(PoseStack poseStack, Camera camera, Collection<Network> networks, Map<UUID, float[]> tints) {
        Vec3 camPos = camera.getPosition();
        Font font = Minecraft.getInstance().font;
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        for (Network network : networks) {
            List<NetworkNode> memberPositions;
            try {
                memberPositions = network.getNodeIndex().getAllNodes().stream().toList();
                memberPositions = new ArrayList<>(memberPositions.stream().toList());
            } catch (Exception ignored) {
                memberPositions = Collections.emptyList();
            }

            // Get the block closest to the camera instead of the network center
            Vec3 targetPos = closestTo(new HashSet<>(memberPositions), camPos);
            if (targetPos == null) continue;

            double distSq = targetPos.distanceToSqr(camPos);
            if (distSq > LABEL_MAX_DIST * LABEL_MAX_DIST) continue;

            float[] t = tints.get(network.getUUID());
            int color = packColor(t[0], t[1], t[2], 1.0f);

            poseStack.pushPose();

            // Translate to the closest block, lifted 1.2 blocks above its center
            poseStack.translate(targetPos.x - camPos.x, targetPos.y - camPos.y + 1.2, targetPos.z - camPos.z);

            // Billboard: face the camera
            poseStack.mulPose(camera.rotation());

            // Scale based on distance so it doesn't get unreadably tiny
            float scale = (float) Math.sqrt(distSq) * 0.0025f;
            scale = Math.max(scale, 0.025f);
            poseStack.scale(-scale, -scale, scale);

            String text = String.format("Net %s | %d members | %d/%d EU",
                    network.getUUID().toString().substring(0, 4),
                    network.getNodeIndex().getAllNodes().size(),
                    network.getStoredEnergyBuffer(),
                    network.getTotalEnergyBuffer());

            float textWidth = font.width(text);
            float x = -textWidth / 2f;

            Matrix4f matrix = poseStack.last().pose();
            font.drawInBatch(text, x, 0, color, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
            font.drawInBatch(text, x, 0, color, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);

            poseStack.popPose();
        }
        bufferSource.endBatch();
    }

    // -------------------------------------------------------------------------
    // Cube geometry helpers
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Cube geometry helpers
    // -------------------------------------------------------------------------

    private static void addFilledCube(BufferBuilder buf, Matrix4f matrix, BlockPos pos, Vec3 cam,
                                      float r, float g, float b, float a) {
        float e = CUBE_EXPAND;
        float x0 = (float) (pos.getX() - cam.x - e);
        float y0 = (float) (pos.getY() - cam.y - e);
        float z0 = (float) (pos.getZ() - cam.z - e);
        float x1 = (float) (pos.getX() - cam.x + 1 + e);
        float y1 = (float) (pos.getY() - cam.y + 1 + e);
        float z1 = (float) (pos.getZ() - cam.z + 1 + e);

        // -Y (bottom)
        quad(buf, matrix, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1, r,g,b,a);
        // +Y (top)
        quad(buf, matrix, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, r,g,b,a);
        // -Z (north)
        quad(buf, matrix, x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0, r,g,b,a);
        // +Z (south)
        quad(buf, matrix, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, r,g,b,a);
        // -X (west)
        quad(buf, matrix, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, r,g,b,a);
        // +X (east)
        quad(buf, matrix, x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1, r,g,b,a);
    }

    private static void quad(BufferBuilder buf, Matrix4f matrix,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float r, float g, float b, float a) {
        buf.vertex(matrix, x0,y0,z0).color(r,g,b,a).endVertex();
        buf.vertex(matrix, x1,y1,z1).color(r,g,b,a).endVertex();
        buf.vertex(matrix, x2,y2,z2).color(r,g,b,a).endVertex();
        buf.vertex(matrix, x3,y3,z3).color(r,g,b,a).endVertex();
    }

    private static void addWireframeCube(BufferBuilder buf, Matrix4f matrix, BlockPos pos, Vec3 cam,
                                         float r, float g, float b, float a) {
        float e = CUBE_EXPAND;
        float x0 = (float) (pos.getX() - cam.x - e);
        float y0 = (float) (pos.getY() - cam.y - e);
        float z0 = (float) (pos.getZ() - cam.z - e);
        float x1 = (float) (pos.getX() - cam.x + 1 + e);
        float y1 = (float) (pos.getY() - cam.y + 1 + e);
        float z1 = (float) (pos.getZ() - cam.z + 1 + e);

        // Bottom ring
        line(buf, matrix, x0,y0,z0, x1,y0,z0, r,g,b,a);
        line(buf, matrix, x1,y0,z0, x1,y0,z1, r,g,b,a);
        line(buf, matrix, x1,y0,z1, x0,y0,z1, r,g,b,a);
        line(buf, matrix, x0,y0,z1, x0,y0,z0, r,g,b,a);
        // Top ring
        line(buf, matrix, x0,y1,z0, x1,y1,z0, r,g,b,a);
        line(buf, matrix, x1,y1,z0, x1,y1,z1, r,g,b,a);
        line(buf, matrix, x1,y1,z1, x0,y1,z1, r,g,b,a);
        line(buf, matrix, x0,y1,z1, x0,y1,z0, r,g,b,a);
        // Verticals
        line(buf, matrix, x0,y0,z0, x0,y1,z0, r,g,b,a);
        line(buf, matrix, x1,y0,z0, x1,y1,z0, r,g,b,a);
        line(buf, matrix, x1,y0,z1, x1,y1,z1, r,g,b,a);
        line(buf, matrix, x0,y0,z1, x0,y1,z1, r,g,b,a);
    }

    private static void line(BufferBuilder buf, Matrix4f matrix,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float r, float g, float b, float a) {
        buf.vertex(matrix, x0,y0,z0).color(r,g,b,a).endVertex();
        buf.vertex(matrix, x1,y1,z1).color(r,g,b,a).endVertex();
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    /** Assigns a stable hue to each network based on its UUID. */
    private static Map<UUID, float[]> buildNetworkTints(Collection<Network> networks) {
        Map<UUID, float[]> map = new HashMap<>();
        for (Network net : networks) {
            // Derive hue from UUID bits — stable across frames
            long bits = net.getUUID().getLeastSignificantBits();
            float hue = (float) ((bits & 0xFFFFFFFFL) / (double) 0x100000000L);
            map.put(net.getUUID(), hsvToRgb(hue, 0.85f, 1.0f));
        }
        return map;
    }

    /** Minimal HSV → RGB (all inputs in [0,1]). */
    private static float[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = v - c;
        float r, g, b;
        int i = (int)(h * 6);
        switch (i % 6) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return new float[]{ r + m, g + m, b + m };
    }

    private static float[] roleColor(NetworkNode node) {
        return COLOR_UNKNOWN;
    }

    private static Vec3 centroidOf(Set<BlockPos> positions) {
        if (positions.isEmpty()) return null;
        double sx = 0, sy = 0, sz = 0;
        for (BlockPos p : positions) {
            sx += p.getX() + 0.5;
            sy += p.getY() + 0.5;
            sz += p.getZ() + 0.5;
        }
        int n = positions.size();
        return new Vec3(sx / n, sy / n, sz / n);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int packColor(float r, float g, float b, float a) {
        return ((int)(a * 255) << 24)
                | ((int)(r * 255) << 16)
                | ((int)(g * 255) << 8)
                |  (int)(b * 255);
    }

    private static String formatEnergy(long eu) {
        if (eu >= 1_000_000) return String.format("%.1fMEU", eu / 1_000_000.0);
        if (eu >= 1_000)     return String.format("%.1fkEU", eu / 1_000.0);
        return eu + " EU";
    }

    private static Vec3 closestTo(Set<NetworkNode> positions, Vec3 camPos) {
        if (positions.isEmpty()) return null;

        double minDistSq = Double.MAX_VALUE;
        Vec3 closestCenter = null;

        for (NetworkNode node : positions) {
            BlockPos p = node.getBlockPos();
            // Get the absolute center of the block
            Vec3 center = new Vec3(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
            double distSq = center.distanceToSqr(camPos);

            // If it's closer than our current record, update the target
            if (distSq < minDistSq) {
                minDistSq = distSq;
                closestCenter = center;
            }
        }

        return closestCenter;
    }

    // Private constructor — static utility class
    private EnergyNetworkDebugRenderer() {}
}
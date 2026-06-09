package com.restonic4.logistics.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.restonic4.logistics.Constants;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.client.ClientNetworkManager;
import com.restonic4.logistics.networks.pathfinding.PathfinderPool;
import com.restonic4.logistics.utils.MathHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class NetworkDebugRenderer {
    private static final double LABEL_MAX_DIST = 64.0;
    private static final float CUBE_ALPHA = 0.35f;

    private static final MeshCache MESH_CACHE = new MeshCache();
    private static final Minecraft MINECRAFT = Minecraft.getInstance();

    // Pathfinding Mock Debug Properties
    public static BlockPos[] pathfindGrid;
    public static BlockPos[] pathfindingSolution;
    public static PathfinderPool pathfinderPool;
    public static BlockPos origin = new BlockPos(0, 80, 0);
    public static BlockPos playerPos = new BlockPos(0, 81, 0);

    static {
        generateDebugPathfindGrid();
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (!Constants.isDebug() || MINECRAFT.level == null || MINECRAFT.player == null) return;

        // 1. Fetch Client-Side Data (Works on both Singleplayer and Multiplayer servers)
        ResourceKey<Level> dimension = MINECRAFT.level.dimension();
        Collection<Network> networks = ClientNetworkManager.getNetworks(dimension);
        if (networks == null || networks.isEmpty()) return;

        Vec3 camPos = camera.getPosition();
        Map<UUID, float[]> networkTints = buildNetworkTints(networks);

        // 2. Setup GL Pipelines
        beginRenderStates();

        // 3. Render Network VBO Geometry meshes
        renderNetworkMeshes(poseStack, camPos, networks, networkTints);

        // 4. Render Mock Pathfinding lines
        renderPathfindingDebug(poseStack, camPos);

        // 5. Tear Down Pipelines & Clean old meshes
        endRenderStates();

        // 6. Draw Overhead Dynamic Spatial Text Labels
        renderLabels(poseStack, camera, networks, networkTints);
    }

    private static void beginRenderStates() {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
    }

    private static void endRenderStates() {
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        MESH_CACHE.cleanup();
    }

    private static void renderNetworkMeshes(PoseStack poseStack, Vec3 camPos, Collection<Network> networks, Map<UUID, float[]> networkTints) {
        for (Network network : networks) {
            Collection<NetworkNode> allNodes = network.getNodeIndex().getAllNodes();
            if (allNodes.isEmpty()) continue;

            // List.copyOf handles thread-safety instantly, dropping the ugly try-catch block
            List<NetworkNode> nodesSnapshot = List.copyOf(allNodes);
            int nodeHash = calculateNodeHash(nodesSnapshot);

            VertexBuffer vbo = MESH_CACHE.get(network.getUUID(), nodeHash);

            if (vbo == null) {
                vbo = buildAndUploadMesh(nodesSnapshot, networkTints.get(network.getUUID()));
                MESH_CACHE.put(network.getUUID(), vbo, nodeHash);
            }

            if (vbo != null) {
                poseStack.pushPose();
                poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
                vbo.bind();
                vbo.drawWithShader(poseStack.last().pose(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
                VertexBuffer.unbind();
                poseStack.popPose();
            }
        }
    }

    private static void renderPathfindingDebug(PoseStack poseStack, Vec3 camPos) {
        if (MINECRAFT.player.isUsingItem()) {
            playerPos = MINECRAFT.player.blockPosition();
        }

        pathfindingSolution = pathfinderPool.findPath(origin, playerPos);

        if (pathfindGrid != null && pathfindGrid.length > 0) {
            renderImmediate(poseStack, camPos, pathfindGrid, 0.7f, 0.9f, 1.0f, 0.25f);
        }

        if (pathfindingSolution != null && pathfindingSolution.length > 0) {
            renderImmediate(poseStack, camPos, pathfindingSolution, 0.4f, 1.0f, 0.4f, 0.5f);
        }
    }

    private static void renderImmediate(PoseStack poseStack, Vec3 camPos, BlockPos[] positions, float r, float g, float b, float a) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        List<BlockPos> posList = Arrays.asList(positions);
        GeometryHelper.culledMesh(buf, poseStack.last().pose(), posList, camPos, r, g, b, a);
        BufferUploader.drawWithShader(buf.end());
    }

    private static void renderLabels(PoseStack poseStack, Camera camera, Collection<Network> networks, Map<UUID, float[]> tints) {
        Vec3 camPos = camera.getPosition();
        RenderingHelper.LABEL_BATCHER.begin(poseStack, camera, LABEL_MAX_DIST);

        for (Network network : networks) {
            Collection<NetworkNode> nodes = network.getNodeIndex().getAllNodes();
            if (nodes.isEmpty()) continue;

            Vec3 targetPos = MathHelper.closestTo(MathHelper.toVec3(nodes), camPos);
            if (targetPos == null) continue;

            float[] tint = tints.get(network.getUUID());
            int color = MathHelper.packColor(tint[0], tint[1], tint[2], 1.0f);

            String labelText = String.format(
                    "Net %s | %s | %d members",
                    network.getUUID().toString().substring(0, 4),
                    network.getResourceLocation(),
                    nodes.size()
            );

            RenderingHelper.LABEL_BATCHER.draw(targetPos.x, targetPos.y, targetPos.z, color, labelText);
        }
        RenderingHelper.LABEL_BATCHER.end();
    }

    private static Map<UUID, float[]> buildNetworkTints(Collection<Network> networks) {
        Map<UUID, float[]> map = new HashMap<>();
        for (Network net : networks) {
            long bits = net.getUUID().getLeastSignificantBits();
            float hue = (float) ((bits & 0xFFFFFFFFL) / (double) 0x100000000L);
            map.put(net.getUUID(), MathHelper.hsvToRgb(hue, 0.85f, 1.0f));
        }
        return map;
    }

    private static VertexBuffer buildAndUploadMesh(Collection<NetworkNode> nodes, float[] tint) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();
        VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f identity = new Matrix4f();
        List<BlockPos> positions = nodes.stream().map(NetworkNode::getBlockPos).toList();
        GeometryHelper.culledMesh(
                buf, identity, positions, Vec3.ZERO,
                tint[0] * 0.4f, tint[1] * 0.4f, tint[2] * 0.4f, CUBE_ALPHA * 0.5f
        );

        vbo.bind();
        vbo.upload(buf.end());
        VertexBuffer.unbind();
        return vbo;
    }

    private static int calculateNodeHash(Collection<NetworkNode> nodes) {
        int hash = 0;
        for (NetworkNode node : nodes) {
            hash += node.getBlockPos().hashCode();
        }
        return hash ^ (nodes.size() * 31);
    }

    private static void generateDebugPathfindGrid() {
        Set<BlockPos> grid = new HashSet<>();
        List<BlockPos> branchPoints = new ArrayList<>();

        Random random = new Random(1337);
        BlockPos cursor = origin;

        grid.add(origin);
        branchPoints.add(origin);

        for (int pipeIdx = 0; pipeIdx < 1000; pipeIdx++) {
            Direction dir = Direction.values()[random.nextInt(6)];
            int length = 4 + random.nextInt(10);

            for (int i = 0; i < length; i++) {
                cursor = cursor.relative(dir);
                grid.add(cursor);
            }

            branchPoints.add(cursor);

            int index1 = random.nextInt(branchPoints.size());
            int index2 = random.nextInt(branchPoints.size());
            int maxIndex = Math.max(index1, index2);

            cursor = branchPoints.get(maxIndex);
        }

        pathfindGrid = grid.toArray(new BlockPos[0]);

        Set<Long> nodes = ConcurrentHashMap.newKeySet();
        for (BlockPos blockPos : pathfindGrid) {
            nodes.add(blockPos.asLong());
        }
        pathfinderPool = new PathfinderPool(nodes::contains);
    }
}
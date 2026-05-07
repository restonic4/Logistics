package com.restonic4.logistics.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.pathfinding.PathfinderPool;
import com.restonic4.logistics.utils.MathHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class NetworkDebugRenderer {
    private static final double LABEL_MAX_DIST = 64.0;
    private static final float CUBE_ALPHA = 0.35f;

    private static final MeshCache MESH_CACHE = new MeshCache();
    private static Minecraft minecraft = Minecraft.getInstance();

    public static BlockPos[] pathfindGrid;
    public static BlockPos[] pathfindingSolution;

    public static PathfinderPool pathfinderPool;
    public static BlockPos origin = new BlockPos(0, 80, 0);
    public static BlockPos playerPos = new BlockPos(0, 81, 0);

    static {
        Set<BlockPos> grid = new HashSet<>();
        List<BlockPos> branchPoints = new ArrayList<>();

        Random random = new Random(1337);
        BlockPos cursor = origin;

        grid.add(origin);
        branchPoints.add(origin); // The origin is our first valid branch point

        for (int pipeIdx = 0; pipeIdx < 1000; pipeIdx++) {
            Direction dir = Direction.values()[random.nextInt(6)];
            int length = 4 + random.nextInt(10);

            for (int i = 0; i < length; i++) {
                cursor = cursor.relative(dir);
                grid.add(cursor);
            }

            // Only add the END of the new pipe to the branch points list
            branchPoints.add(cursor);

            int index1 = random.nextInt(branchPoints.size());
            int index2 = random.nextInt(branchPoints.size());
            int biasedIndex = Math.max(index1, index2);

            cursor = branchPoints.get(biasedIndex);
        }

        pathfindGrid = grid.toArray(new BlockPos[0]);

        Set<Long> nodes = ConcurrentHashMap.newKeySet();
        for (BlockPos blockPos : pathfindGrid) {
            nodes.add(blockPos.asLong());
        }
        pathfinderPool = new PathfinderPool(nodes::contains);
    }

    public static void render(PoseStack poseStack, Camera camera) {
        try {
            if (minecraft.level == null || minecraft.player == null) return;
            if (minecraft.getSingleplayerServer() == null) return;

            ServerLevel serverLevel = minecraft.getSingleplayerServer().getLevel(minecraft.level.dimension());
            if (serverLevel == null) return;

            if (minecraft.player.isUsingItem()) {
                playerPos = minecraft.player.blockPosition();
            }

            pathfindingSolution = pathfinderPool.findPath(origin, playerPos);

            NetworkManager manager = NetworkManager.get(serverLevel);
            Collection<Network> networks = manager.getAllNetworks();
            if (networks.isEmpty()) return;

            Vec3 camPos = camera.getPosition();
            Map<UUID, float[]> networkTints = buildNetworkTints(networks);

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();

            for (Network network : networks) {
                List<NetworkNode> nodes;
                try {
                    nodes = new ArrayList<>(network.getNodeIndex().getAllNodes());
                } catch (Exception e) {
                    continue;
                }

                int nodeHash = calculateNodeHash(nodes);
                VertexBuffer vbo = MESH_CACHE.get(network.getUUID(), nodeHash);

                if (vbo == null && !nodes.isEmpty()) {
                    vbo = buildAndUploadMesh(nodes, networkTints.get(network.getUUID()));
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

            if (pathfindGrid != null && pathfindGrid.length > 0) {
                renderImmediate(poseStack, camPos, pathfindGrid, 0.7f, 0.9f, 1.0f, 0.25f);
            }

            if (pathfindingSolution != null && pathfindingSolution.length > 0) {
                renderImmediate(poseStack, camPos, pathfindingSolution, 0.4f, 1.0f, 0.4f, 0.5f);
            }

            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();

            MESH_CACHE.cleanup();

            renderLabels(poseStack, camera, networks, networkTints);
        } catch (Exception ignored) {

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

    private static int calculateNodeHash(Collection<NetworkNode> nodes) {
        int hash = 0;
        for (NetworkNode node : nodes) {
            hash += node.getBlockPos().hashCode();
        }
        return hash ^ (nodes.size() * 31);
    }

    private static void renderLabels(PoseStack poseStack, Camera camera, Collection<Network> networks, Map<UUID, float[]> tints) {
        Vec3 camPos = camera.getPosition();
        RenderingHelper.LABEL_BATCHER.begin(poseStack, camera, LABEL_MAX_DIST);

        for (Network network : networks) {
            Collection<NetworkNode> nodes = network.getNodeIndex().getAllNodes();

            Vec3 targetPos = MathHelper.closestTo(MathHelper.toVec3(nodes), camPos);
            if (targetPos == null) continue;

            float[] t = tints.get(network.getUUID());
            int color = MathHelper.packColor(t[0], t[1], t[2], 1.0f);

            RenderingHelper.LABEL_BATCHER.draw(
                    targetPos.x, targetPos.y, targetPos.z,
                    color,
                    String.format(
                            "Net %s | %s | %d members",
                            network.getUUID().toString().substring(0, 4),
                            network.getResourceLocation(),
                            network.getNodeIndex().getAllNodes().size()
                    )
            );
        }
        RenderingHelper.LABEL_BATCHER.end();
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
}
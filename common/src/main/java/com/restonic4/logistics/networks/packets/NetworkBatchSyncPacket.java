package com.restonic4.logistics.networks.packets;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.networking.S2CPacket;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.client.ClientNetworkManager;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NetworkBatchSyncPacket implements S2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "network_batch_sync");

    private UUID networkUuid;
    private ResourceLocation networkType;
    private ResourceKey<Level> dimension;
    private boolean isFirstBatch;
    private List<NetworkNode> nodesInBatch;

    // Server-side constructor
    public NetworkBatchSyncPacket(Network network, boolean isFirstBatch, List<NetworkNode> nodesInBatch) {
        this.networkUuid = network.getUUID();
        this.networkType = network.getResourceLocation();
        this.dimension = network.getServerLevel().dimension();
        this.isFirstBatch = isFirstBatch;
        this.nodesInBatch = nodesInBatch;
    }

    // Client decoder constructor
    public NetworkBatchSyncPacket(FriendlyByteBuf buf) {
        this.networkUuid = buf.readUUID();
        this.networkType = buf.readResourceLocation();
        this.dimension = buf.readResourceKey(Registries.DIMENSION);
        this.isFirstBatch = buf.readBoolean();

        int nodeCount = buf.readVarInt();
        this.nodesInBatch = new ArrayList<>(nodeCount);

        for (int i = 0; i < nodeCount; i++) {
            ResourceLocation nodeType = buf.readResourceLocation();
            var typeObj = NodeTypeRegistry.get(nodeType);

            NetworkNode node = typeObj.create(BlockPos.ZERO);
            node.readSyncData(buf);
            this.nodesInBatch.add(node);
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(networkUuid);
        buf.writeResourceLocation(networkType);
        buf.writeResourceKey(dimension);
        buf.writeBoolean(isFirstBatch);

        buf.writeVarInt(nodesInBatch.size());
        for (NetworkNode node : nodesInBatch) {
            buf.writeResourceLocation(node.getResourceLocation());
            node.writeSyncData(buf);
        }
    }

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> {
            Network network = ClientNetworkManager.getNetwork(dimension, networkUuid);
            if (network == null || isFirstBatch) {
                network = Network.createEmptyOnClient(networkUuid, networkType, dimension);
                ClientNetworkManager.putNetwork(dimension, network);
            }

            for (NetworkNode node : nodesInBatch) {
                network.getNodeIndex().register(node);
            }
        });
    }

    @Override
    public ResourceLocation getId() { return ID; }

    // Helpers

    public static void syncAllNetworksToPlayer(MinecraftServer server, ServerPlayer player) {
        for (ServerLevel level : server.getAllLevels()) {
            syncAllNetworksInLevelToPlayer(level, player);
        }
    }

    public static void syncAllNetworksInLevelToPlayer(ServerLevel serverLevel, ServerPlayer player) {
        ResourceLocation dim = serverLevel.dimension().location();
        String playerName = player.getDisplayName().getString();

        Constants.LOG.info("Syncing all networks for {} at {}", playerName, dim);

        NetworkManager manager = NetworkManager.get(serverLevel);
        List<Network> allNetworks = manager.getAllNetworks().stream().toList();
        int totalNetworks = allNetworks.size();
        int networkIdx = 0;

        for (Network network : allNetworks) {
            networkIdx++;
            List<NetworkNode> allNodes = new ArrayList<>(network.getNodeIndex().getAllNodes());

            int totalNodes = allNodes.size();
            int chunkSize = 256;
            int totalBatches = (totalNodes + chunkSize - 1) / chunkSize;
            boolean isFirst = true;

            for (int batchNum = 0; batchNum < totalBatches; batchNum++) {
                int start = batchNum * chunkSize;
                int end = Math.min(start + chunkSize, totalNodes);
                List<NetworkNode> batch = allNodes.subList(start, end);

                ServerNetworking.sendToClient(player, new NetworkBatchSyncPacket(network, isFirst, batch));

                Constants.LOG.debug(
                        "Net {}/{} | Batch {}/{} | nodes {}-{} of {} | {} in {}",
                        networkIdx, totalNetworks,
                        batchNum + 1, totalBatches,
                        start, end - 1, totalNodes,
                        playerName, dim
                );

                isFirst = false;
            }
        }

        Constants.LOG.info("Done for {} at {}", playerName, dim);
    }
}
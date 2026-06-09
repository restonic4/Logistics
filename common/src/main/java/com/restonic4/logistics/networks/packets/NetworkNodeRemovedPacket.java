package com.restonic4.logistics.networks.packets;

import com.restonic4.logistics.networking.S2CPacket;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.client.ClientNetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class NetworkNodeRemovedPacket implements S2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "node_removed");

    private UUID networkUuid;
    private UUID nodeUuid;
    private ResourceKey<Level> dimension;

    public NetworkNodeRemovedPacket(Network network, NetworkNode node) {
        this.networkUuid = network.getUUID();
        this.nodeUuid = node.getUUID();
        this.dimension = network.getServerLevel().dimension();
    }

    public NetworkNodeRemovedPacket(FriendlyByteBuf buf) {
        this.networkUuid = buf.readUUID();
        this.nodeUuid = buf.readUUID();
        this.dimension = buf.readResourceKey(Registries.DIMENSION);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(networkUuid);
        buf.writeUUID(nodeUuid);
        buf.writeResourceKey(dimension);
    }

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> {
            Network network = ClientNetworkManager.getNetwork(dimension, networkUuid);
            if (network != null) {
                NetworkNode node = network.getNodeIndex().findByUUID(nodeUuid);
                if (node != null) {
                    network.getNodeIndex().unregister(node);
                }
            }
        });
    }

    @Override
    public ResourceLocation getId() { return ID; }
}
package com.restonic4.logistics.networks.packets;

import com.restonic4.logistics.networking.S2CPacket;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.client.ClientNetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class NetworkDestroyedPacket implements S2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "network_destroyed");

    private UUID networkUuid;
    private ResourceKey<Level> dimension;

    public NetworkDestroyedPacket(Network network) {
        this.networkUuid = network.getUUID();
        this.dimension = network.getServerLevel().dimension();
    }

    public NetworkDestroyedPacket(FriendlyByteBuf buf) {
        this.networkUuid = buf.readUUID();
        this.dimension = buf.readResourceKey(Registries.DIMENSION);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(networkUuid);
        buf.writeResourceKey(dimension);
    }

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> {
            ClientNetworkManager.removeNetwork(dimension, networkUuid);
        });
    }

    @Override
    public ResourceLocation getId() { return ID; }
}
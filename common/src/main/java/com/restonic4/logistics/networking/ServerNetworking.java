package com.restonic4.logistics.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ServerNetworking {
    public static void sendToClient(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
        player.connection.send(new ClientboundCustomPayloadPacket(id, buf));
    }

    public static void sendToAllInLevel(ServerLevel level, ResourceLocation id, FriendlyByteBuf buf) {
        ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(id, buf);
        for (ServerPlayer player : level.players()) {
            player.connection.send(packet);
        }
    }
}

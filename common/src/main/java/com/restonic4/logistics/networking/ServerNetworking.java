package com.restonic4.logistics.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ServerNetworking {
    public static void sendToClient(ServerPlayer player, S2CPacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(buf);
        player.connection.send(new ClientboundCustomPayloadPacket(packet.getId(), buf));
    }

    public static void sendToAllInLevel(ServerLevel level, S2CPacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(buf);

        ClientboundCustomPayloadPacket vanillaPacket = new ClientboundCustomPayloadPacket(packet.getId(), buf);
        for (ServerPlayer player : level.players()) {
            player.connection.send(vanillaPacket);
        }
    }
}

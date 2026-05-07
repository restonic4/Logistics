package com.restonic4.logistics.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;

public class ClientNetworking {
    public static void sendToServer(ResourceLocation id, FriendlyByteBuf buf) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(id, buf));
        }
    }
}

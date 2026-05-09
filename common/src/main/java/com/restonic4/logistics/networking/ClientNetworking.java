package com.restonic4.logistics.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;

public class ClientNetworking {
    public static void sendToServer(C2SPacket packet) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            packet.write(buf);
            connection.send(new ServerboundCustomPayloadPacket(packet.getId(), buf));
        }
    }
}

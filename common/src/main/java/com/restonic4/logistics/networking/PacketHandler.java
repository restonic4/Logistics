package com.restonic4.logistics.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface PacketHandler {
    void handle(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf);
}

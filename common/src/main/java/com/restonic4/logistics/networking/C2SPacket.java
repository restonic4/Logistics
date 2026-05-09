package com.restonic4.logistics.networking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public interface C2SPacket extends BasePacket {
    void handle(MinecraftServer server, ServerPlayer player);
}

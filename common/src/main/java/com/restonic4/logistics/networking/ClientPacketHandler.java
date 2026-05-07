package com.restonic4.logistics.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

@FunctionalInterface
public interface ClientPacketHandler {
    void handle(Minecraft client, FriendlyByteBuf buf);
}

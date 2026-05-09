package com.restonic4.logistics.networking;

import net.minecraft.client.Minecraft;

public interface S2CPacket extends BasePacket {
    void handle(Minecraft client);
}
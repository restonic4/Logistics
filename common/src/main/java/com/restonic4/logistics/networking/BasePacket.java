package com.restonic4.logistics.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public interface BasePacket {
    void write(FriendlyByteBuf buf);
    ResourceLocation getId();
}

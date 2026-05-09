package com.restonic4.logistics.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class NetworkingRegistry {
    private static final Map<ResourceLocation, Function<FriendlyByteBuf, C2SPacket>> SERVER_RECEIVERS = new HashMap<>();
    private static final Map<ResourceLocation, Function<FriendlyByteBuf, S2CPacket>> CLIENT_RECEIVERS = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends C2SPacket> void registerServerTargetedPacket(ResourceLocation id, Function<FriendlyByteBuf, T> decoder) {
        SERVER_RECEIVERS.put(id, (Function<FriendlyByteBuf, C2SPacket>) decoder);
    }

    @SuppressWarnings("unchecked")
    public static <T extends S2CPacket> void registerClientTargetedPacket(ResourceLocation id, Function<FriendlyByteBuf, T> decoder) {
        CLIENT_RECEIVERS.put(id, (Function<FriendlyByteBuf, S2CPacket>) decoder);
    }

    public static Function<FriendlyByteBuf, C2SPacket> getC2SDecoder(ResourceLocation id) { return SERVER_RECEIVERS.get(id); }
    public static Function<FriendlyByteBuf, S2CPacket> getS2CDecoder(ResourceLocation id) { return CLIENT_RECEIVERS.get(id); }
}

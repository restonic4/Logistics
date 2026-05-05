package com.restonic4.logistics.networking;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class NetworkingRegistry {
    private static final Map<ResourceLocation, PacketHandler> SERVER_RECEIVERS = new HashMap<>();
    private static final Map<ResourceLocation, ClientPacketHandler> CLIENT_RECEIVERS = new HashMap<>();

    public static void registerServerListener(ResourceLocation id, PacketHandler handler) {
        SERVER_RECEIVERS.put(id, handler);
    }

    public static void registerClientListener(ResourceLocation id, ClientPacketHandler handler) {
        CLIENT_RECEIVERS.put(id, handler);
    }

    public static PacketHandler getServerHandler(ResourceLocation id) { return SERVER_RECEIVERS.get(id); }
    public static ClientPacketHandler getClientHandler(ResourceLocation id) { return CLIENT_RECEIVERS.get(id); }
}

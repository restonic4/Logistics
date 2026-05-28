package com.restonic4.logistics.blocks.computer.protection;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.protector.data_types.ClientProtectionCache;
import com.restonic4.logistics.blocks.protector.data_types.ProtectionZone;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record ProtectionCacheSyncPacket(Map<ResourceLocation, List<ProtectionZone>> zonesByDimension) implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("protection_cache_sync");

    @Override
    public void handle(Minecraft client) {
        ClientProtectionCache.update(zonesByDimension);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(zonesByDimension.size());
        for (var e : zonesByDimension.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeCollection(e.getValue(), (b, z) -> z.netWrite(b));
        }
    }

    public static ProtectionCacheSyncPacket read(FriendlyByteBuf buf) {
        Map<ResourceLocation, List<ProtectionZone>> map = new HashMap<>();
        int dims = buf.readInt();
        for (int i = 0; i < dims; i++) {
            ResourceLocation dim = buf.readResourceLocation();
            map.put(dim, buf.readList(ProtectionZone::netRead));
        }
        return new ProtectionCacheSyncPacket(map);
    }

    @Override
    public ResourceLocation getId() { return ID; }
}
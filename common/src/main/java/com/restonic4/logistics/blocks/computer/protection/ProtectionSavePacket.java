package com.restonic4.logistics.blocks.computer.protection;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.protector.ProtectorNode;
import com.restonic4.logistics.blocks.protector.data_types.ProtectorData;
import com.restonic4.logistics.blocks.protector.data_types.ProtectionZone;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public record ProtectionSavePacket(BlockPos computerNodePos, Map<UUID, ProtectorData> protectors) implements C2SPacket {
    public static final ResourceLocation ID = Logistics.id("protection_save");

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        ResourceLocation dim = level.dimension().location();
        NetworkManager mgr = NetworkManager.get(level);

        // Apply edits
        for (var entry : protectors.entrySet()) {
            NetworkNode node = mgr.getNodeByUUID(entry.getKey());
            if (node instanceof ProtectorNode protector) {
                ProtectorData data = entry.getValue();
                data.validate();
                protector.setRadius(data.getRadius());
                protector.setCreative(data.isCreative());
                protector.setRoles(data.getRoles());
            }
        }

        // Rebuild server cache for this dimension
        List<ProtectionZone> zones = new ArrayList<>();
        for (Network network : mgr.getAllNetworks()) {
            if (network instanceof EnergyNetwork energyNetwork) {
                for (ProtectorNode protector : energyNetwork.getProtectors()) {
                    zones.add(new ProtectionZone(
                            protector.getUUID(),
                            protector.getBlockPos(),
                            protector.getRadius(),
                            protector.isCreative(),
                            protector.getRoles()
                    ));
                }
            }
        }
        ServerProtectionCache.updateDimension(dim, zones);

        // Broadcast lightweight cache to all clients in this dimension
        Map<ResourceLocation, List<ProtectionZone>> wrapped = new HashMap<>();
        wrapped.put(dim, zones);
        ServerNetworking.sendToAllInLevel(level, new ProtectionCacheSyncPacket(wrapped));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerNodePos);
        buf.writeInt(protectors.size());
        for (var e : protectors.entrySet()) {
            buf.writeUUID(e.getKey());
            e.getValue().netWrite(buf);
        }
    }

    public static ProtectionSavePacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int count = buf.readInt();
        Map<UUID, ProtectorData> map = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            map.put(buf.readUUID(), ProtectorData.netRead(buf));
        }
        return new ProtectionSavePacket(pos, map);
    }

    @Override
    public ResourceLocation getId() { return ID; }
}
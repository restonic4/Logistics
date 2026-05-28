package com.restonic4.logistics.blocks.protector.data_types;

import com.restonic4.logistics.blocks.protector.ProtectorNode;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class ServerProtectionCache {
    private static final Map<ResourceLocation, List<ProtectionZone>> ZONES = new HashMap<>();

    public static void updateDimension(ResourceLocation dim, List<ProtectionZone> zones) {
        ZONES.put(dim, List.copyOf(zones));
    }

    /** Rebuilds the cache for a single dimension and returns the built zone list. */
    public static List<ProtectionZone> rebuildForLevel(ServerLevel level) {
        ResourceLocation dim = level.dimension().location();
        NetworkManager mgr = NetworkManager.get(level);
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

        updateDimension(dim, zones);
        return zones;
    }

    public static FlagData getFlagState(ResourceLocation dim, BlockPos target, Player player, String flagId) {
        List<ProtectionZone> list = ZONES.get(dim);
        if (list == null) return null;

        for (ProtectionZone zone : list) {
            if (zone.contains(target)) {
                return zone.resolveFlag(player, flagId).orElse(null);
            }
        }
        return null;
    }

    public static boolean isActionAllowed(ResourceLocation dim, BlockPos target, Player player, String flagId) {
        FlagData fd = getFlagState(dim, target, player, flagId);
        return fd == null || !fd.enabled() || !fd.actionType().equals(ActionType.DENY.name());
    }
}
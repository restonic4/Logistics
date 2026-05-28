package com.restonic4.logistics.blocks.protector.data_types;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class ClientProtectionCache {
    private static final Map<ResourceLocation, List<ProtectionZone>> ZONES = new HashMap<>();

    public static void update(Map<ResourceLocation, List<ProtectionZone>> zones) {
        ZONES.clear();
        ZONES.putAll(zones);
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
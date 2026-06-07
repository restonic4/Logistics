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

    public static boolean hasPower(ResourceLocation dim, BlockPos target) {
        List<ProtectionZone> list = ZONES.get(dim);
        if (list == null) return false;
        for (ProtectionZone zone : list) {
            if (zone.contains(target) && zone.powered()) return true;
        }
        return false;
    }

    public static FlagData getFlagState(ResourceLocation dim, BlockPos target, Player player, String flagId) {
        List<ProtectionZone> list = ZONES.get(dim);
        if (list == null) return null;

        List<FlagData> matches = new ArrayList<>();
        for (ProtectionZone zone : list) {
            if (zone.contains(target) && zone.powered()) {
                zone.resolveFlag(player, flagId).ifPresent(matches::add);
            }
        }
        return FlagData.merge(matches);
    }

    public static boolean isActionAllowed(ResourceLocation dim, BlockPos target, Player player, String flagId) {
        FlagData fd = getFlagState(dim, target, player, flagId);
        if (fd == null || !fd.enabled()) return true;
        try {
            ActionType action = ActionType.valueOf(fd.actionType());
            return action != ActionType.DENY && action != ActionType.DAMAGE && action != ActionType.MESSAGE;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }
}
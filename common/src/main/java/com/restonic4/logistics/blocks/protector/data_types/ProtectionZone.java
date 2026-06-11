package com.restonic4.logistics.blocks.protector.data_types;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.Logistics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.Optional;

public record ProtectionZone(UUID nodeId, BlockPos pos, int radius, boolean creative, String name, List<RoleData> roles, boolean powered) {

    public ProtectionZone {
        List<RoleData> sorted = new ArrayList<>(roles);
        sorted.sort(Comparator.<RoleData>comparingInt(r ->
                r.roleType() == RoleData.RoleType.DEFAULT ? Integer.MAX_VALUE : r.order()
        ).thenComparing(r -> r.roleType() == RoleData.RoleType.DEFAULT ? 1 : 0));
        roles = List.copyOf(sorted);
    }

    public boolean contains(BlockPos target) {
        return target.distSqr(pos) <= (double) radius * radius;
    }

    public Optional<FlagData> resolveFlag(Player player, String flagId) {
        Optional<FlagDefinition> def = FlagRegistry.byId(flagId);
        if (def.isEmpty()) {
            return Optional.empty();
        }
        if (def.get().category() == FlagDefinition.FlagCategory.CREATIVE && !creative) {
            return Optional.empty();
        }

        RoleData fallback = null;

        if (player != null) {
            for (RoleData role : roles) {
                if (role.roleType() == RoleData.RoleType.DEFAULT) {
                    fallback = role;
                    continue;
                }
                boolean member = role.players().stream().anyMatch(p -> p.id().equals(player.getUUID()));
                if (member) {
                    FlagData fd = role.flags().get(flagId);
                    if (fd == null) {
                        return Optional.of(new FlagData(false, ActionType.DENY.name(), 0, ""));
                    }
                    return Optional.of(fd);
                }
            }
        } else {
            for (RoleData role : roles) {
                FlagData fd = role.flags().get(flagId);
                if (fd != null) return Optional.of(fd);
            }
        }

        if (fallback != null) {
            FlagData fd = fallback.flags().get(flagId);
            if (fd != null) {
                return Optional.of(fd);
            }
        }

        return Optional.empty();
    }

    public boolean isActionAllowed(Player player, String flagId) {
        Optional<FlagData> fd = resolveFlag(player, flagId);
        if (fd.isEmpty()) return true;
        FlagData data = fd.get();
        return !data.enabled() || !data.actionType().equals(ActionType.DENY.name());
    }

    public void netWrite(FriendlyByteBuf buf) {
        buf.writeUUID(nodeId);
        buf.writeBlockPos(pos);
        buf.writeInt(radius);
        buf.writeBoolean(creative);
        buf.writeUtf(name);
        buf.writeCollection(roles, (b, r) -> r.netWrite(b));
        buf.writeBoolean(powered);
    }

    public static ProtectionZone netRead(FriendlyByteBuf buf) {
        UUID nodeId = buf.readUUID();
        BlockPos pos = buf.readBlockPos();
        int radius = buf.readInt();
        boolean creative = buf.readBoolean();
        String name = buf.readUtf();
        List<RoleData> roles = buf.readList(RoleData::netRead);
        boolean powered = buf.readBoolean();
        return new ProtectionZone(nodeId, pos, radius, creative, name, roles, powered);
    }
}
package com.restonic4.logistics.blocks.protector.data_types;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.*;

public record RoleData(
        UUID id, String name, int order, String iconRl, RoleType roleType,
        List<PlayerData> players, Map<String, FlagData> flags
) {
    public boolean isDefault() { return roleType == RoleType.DEFAULT; }

    public void netWrite(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(name);
        buf.writeInt(order);
        buf.writeUtf(iconRl);
        buf.writeEnum(roleType);
        buf.writeCollection(players, (b, p) -> p.netWrite(b));
        buf.writeMap(flags, FriendlyByteBuf::writeUtf, (b, f) -> f.netWrite(b));
    }

    public static RoleData netRead(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        String name = buf.readUtf();
        int order = buf.readInt();
        String iconRl = buf.readUtf();
        RoleType roleType = buf.readEnum(RoleType.class);
        List<PlayerData> players = buf.readList(PlayerData::netRead);
        Map<String, FlagData> flags = buf.readMap(FriendlyByteBuf::readUtf, FlagData::netRead);
        return new RoleData(id, name, order, iconRl, roleType, players, flags);
    }

    public void nbtWrite(CompoundTag tag) {
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putInt("order", order);
        tag.putString("iconRl", iconRl);
        tag.putString("roleType", roleType.name());
        ListTag playersTag = new ListTag();
        for (PlayerData player : players) {
            CompoundTag playerCompound = new CompoundTag();
            player.nbtWrite(playerCompound);
            playersTag.add(playerCompound);
        }
        tag.put("players", playersTag);
        CompoundTag flagsTag = new CompoundTag();
        for (Map.Entry<String, FlagData> entry : flags.entrySet()) {
            CompoundTag flagCompound = new CompoundTag();
            entry.getValue().nbtWrite(flagCompound);
            flagsTag.put(entry.getKey(), flagCompound);
        }
        tag.put("flags", flagsTag);
    }

    public static RoleData nbtRead(CompoundTag tag) {
        UUID id = tag.getUUID("id");
        String name = tag.getString("name");
        int order = tag.getInt("order");
        String iconRl = tag.getString("iconRl");
        RoleType roleType = RoleType.valueOf(tag.getString("roleType"));
        List<PlayerData> players = new ArrayList<>();
        ListTag playersTag = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < playersTag.size(); i++) {
            players.add(PlayerData.nbtRead(playersTag.getCompound(i)));
        }
        Map<String, FlagData> flags = new HashMap<>();
        CompoundTag flagsTag = tag.getCompound("flags");
        for (String key : flagsTag.getAllKeys()) {
            flags.put(key, FlagData.nbtRead(flagsTag.getCompound(key)));
        }
        return new RoleData(id, name, order, iconRl, roleType, players, flags);
    }

    public enum RoleType { CUSTOM, DEFAULT }
}
package com.restonic4.logistics.blocks.protector.data_types;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record PlayerData(UUID id, String username) {
    public void netWrite(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(username);
    }

    public static PlayerData netRead(FriendlyByteBuf buf) {
        return new PlayerData(buf.readUUID(), buf.readUtf());
    }

    public void nbtWrite(CompoundTag tag) {
        tag.putUUID("id", id);
        tag.putString("username", username);
    }

    public static PlayerData nbtRead(CompoundTag tag) {
        return new PlayerData(tag.getUUID("id"), tag.getString("username"));
    }
}

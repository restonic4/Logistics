package com.restonic4.logistics.blocks.protector.data_types;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record FlagData(boolean enabled, String actionType, double damageValue, String message) {
    public void netWrite(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeUtf(actionType);
        buf.writeDouble(damageValue);
        buf.writeUtf(message);
    }

    public static FlagData netRead(FriendlyByteBuf buf) {
        return new FlagData(buf.readBoolean(), buf.readUtf(), buf.readDouble(), buf.readUtf());
    }

    public void nbtWrite(CompoundTag tag) {
        tag.putBoolean("enabled", enabled);
        tag.putString("actionType", actionType);
        tag.putDouble("damageValue", damageValue);
        tag.putString("message", message);
    }

    public static FlagData nbtRead(CompoundTag tag) {
        return new FlagData(tag.getBoolean("enabled"), tag.getString("actionType"), tag.getDouble("damageValue"), tag.getString("message"));
    }
}
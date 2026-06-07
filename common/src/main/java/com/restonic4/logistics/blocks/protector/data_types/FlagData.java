package com.restonic4.logistics.blocks.protector.data_types;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

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

    public static FlagData merge(List<FlagData> flags) {
        if (flags == null || flags.isEmpty()) return null;

        boolean anyEnabled = false;
        ActionType bestAction = null;
        double maxDamage = 0;
        String message = "";

        for (FlagData fd : flags) {
            if (!fd.enabled()) continue;
            anyEnabled = true;

            ActionType action;
            try {
                action = ActionType.valueOf(fd.actionType());
            } catch (IllegalArgumentException e) {
                continue;
            }

            if (bestAction == null || action.restrictiveness() > bestAction.restrictiveness()) {
                bestAction = action;
                maxDamage = fd.damageValue();
                message = fd.message();
            } else if (action.restrictiveness() == bestAction.restrictiveness()) {
                if (action == ActionType.DAMAGE) {
                    maxDamage = Math.max(maxDamage, fd.damageValue());
                }
                if (message.isEmpty() && !fd.message().isEmpty()) {
                    message = fd.message();
                }
            }
        }

        if (!anyEnabled) return null;
        return new FlagData(true, bestAction.name(), maxDamage, message);
    }
}
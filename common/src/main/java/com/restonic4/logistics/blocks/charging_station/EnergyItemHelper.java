package com.restonic4.logistics.blocks.charging_station;

import com.restonic4.logistics.blocks.battery.BatteryBlockItem;
import com.restonic4.logistics.blocks.battery.BatteryNode;
import com.restonic4.logistics.experiment.KineticCrystalShardItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class EnergyItemHelper {
    public static boolean isEnergyItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof KineticCrystalShardItem) return true;
        if (stack.getItem() instanceof BatteryBlockItem) return true;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("stored_energy", Tag.TAG_LONG);
    }

    public static long getMaxEnergy(ItemStack stack) {
        if (stack.getItem() instanceof KineticCrystalShardItem) return KineticCrystalShardItem.TOTAL;
        if (stack.getItem() instanceof BatteryBlockItem) return BatteryNode.MAX_STORAGE;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("max_storage", Tag.TAG_LONG)) {
            return tag.getLong("max_storage");
        }
        return 0;
    }

    public static long getStoredEnergy(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        return tag.getLong("stored_energy");
    }

    public static void setStoredEnergy(ItemStack stack, long energy) {
        long max = getMaxEnergy(stack);
        stack.getOrCreateTag().putLong("stored_energy", Math.min(energy, max));
    }
}
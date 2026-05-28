package com.restonic4.logistics.experiment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BrokenKineticCrystalShardItem extends Item implements DyeableLeatherItem {
    public static final String COLOR_KEY = "color";
    public static final int TOTAL = 10000;

    public BrokenKineticCrystalShardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public int getColor(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(COLOR_KEY)) {
            return tag.getInt(COLOR_KEY);
        }
        return 0xFFFFFF;
    }

    public void setColor(ItemStack stack, int color) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(COLOR_KEY, color);
    }
}

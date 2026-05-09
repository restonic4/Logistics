package com.restonic4.logistics.networks.nodes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public record InventoryDelta(int index, ItemStack stack) {
    public InventoryDelta(int index, ItemStack stack) {
        this.index = index;
        this.stack = stack.copy();
    }

    @Override
    public ItemStack stack() {
        return stack.copy();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("index", index);
        tag.put("stack", stack.save(new CompoundTag()));
        return tag;
    }

    public static InventoryDelta load(CompoundTag tag) {
        int index = tag.getInt("index");
        ItemStack stack = ItemStack.of(tag.getCompound("stack"));
        return new InventoryDelta(index, stack);
    }
}

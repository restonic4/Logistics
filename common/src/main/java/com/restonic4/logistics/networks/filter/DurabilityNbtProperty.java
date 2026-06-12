package com.restonic4.logistics.networks.filter;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Remaining durability of damageable items (tools, armor, ...). */
public class DurabilityNbtProperty extends NbtProperty {
    public DurabilityNbtProperty(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean appliesTo(ItemStack stack) {
        return stack.isDamageableItem();
    }

    @Override
    public double getValue(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    @Override
    public double getMaxValue(ItemStack stack) {
        return stack.getMaxDamage();
    }
}

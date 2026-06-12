package com.restonic4.logistics.networks.filter;

import com.restonic4.logistics.blocks.charging_station.EnergyItemHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Stored energy of energy-carrying items (kinetic crystal shards, battery items, or anything
 * with a {@code stored_energy} tag), as exposed by {@link EnergyItemHelper}.
 */
public class EnergyNbtProperty extends NbtProperty {
    public EnergyNbtProperty(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean appliesTo(ItemStack stack) {
        return EnergyItemHelper.isEnergyItem(stack) && EnergyItemHelper.getMaxEnergy(stack) > 0;
    }

    @Override
    public double getValue(ItemStack stack) {
        return EnergyItemHelper.getStoredEnergy(stack);
    }

    @Override
    public double getMaxValue(ItemStack stack) {
        return EnergyItemHelper.getMaxEnergy(stack);
    }
}

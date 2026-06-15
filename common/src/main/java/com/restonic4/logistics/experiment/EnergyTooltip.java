package com.restonic4.logistics.experiment;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface EnergyTooltip {
    default CompoundTag appendEnergyTooltip(
            @NotNull ItemStack stack,
            @NotNull List<Component> tooltipComponents,
            int total
    ) {
        CompoundTag tag = stack.getTag();

        long storedEnergy = tag != null ? tag.getLong("stored_energy") : 0L;
        float energyPercent = ((float) storedEnergy / total) * 100.0f;

        tooltipComponents.add(
                Component.literal("Energy: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(String.format("%.1f%%", energyPercent)).withStyle(ChatFormatting.AQUA))
        );

        return tag;
    }
}

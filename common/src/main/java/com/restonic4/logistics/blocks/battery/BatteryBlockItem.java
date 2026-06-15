package com.restonic4.logistics.blocks.battery;

import com.restonic4.logistics.experiment.EnergyTooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BatteryBlockItem extends BlockItem implements EnergyTooltip {
    public BatteryBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long stored = stack.getOrCreateTag().getLong("stored_energy");
        return Math.round(13.0f * stored / BatteryNode.MAX_STORAGE);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        long stored = stack.getOrCreateTag().getLong("stored_energy");
        float t = (float) stored / BatteryNode.MAX_STORAGE;
        int r = (int) ((1f - t) * 255);
        int g = (int) (t * 255);
        return (r << 16) | (g << 8);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        long storedEnergy = context.getItemInHand().getOrCreateTag().getLong("stored_energy");

        if (!context.getLevel().isClientSide()) {
            BatteryBlock block = (BatteryBlock) getBlock();
            block.setPendingEnergy(context.getClickedPos(), storedEnergy);
        }

        return super.place(context);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        this.appendEnergyTooltip(stack, tooltipComponents, (int) BatteryNode.MAX_STORAGE);
    }
}
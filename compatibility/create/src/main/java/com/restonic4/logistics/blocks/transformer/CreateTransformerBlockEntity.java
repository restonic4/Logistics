package com.restonic4.logistics.blocks.transformer;

import com.restonic4.logistics.CreateCommonCompatibility;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class CreateTransformerBlockEntity extends KineticBlockEntity {
    public CreateTransformerBlockEntity(BlockPos pos, BlockState state) {
        super(CreateCommonCompatibility.CREATE_TRANSFORMER.getBlockEntityType(CreateTransformerBlockEntity.class), pos, state);
    }

    public float getAvailableStress() {
        return this.capacity - this.stress;
    }

    public long getEnergy() {
        return (long) Math.floor(getAvailableStress() * CreateCommonCompatibility.CONVERSION_RATE);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Energy: " + getEnergy()).withStyle(ChatFormatting.GOLD));
        added = true;

        return added;
    }
}

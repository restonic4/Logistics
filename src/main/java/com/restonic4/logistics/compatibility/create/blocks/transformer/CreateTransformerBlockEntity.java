package com.restonic4.logistics.compatibility.create.blocks.transformer;

import com.restonic4.logistics.compatibility.create.CreateCompatibility;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.KineticScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CreateTransformerBlockEntity extends KineticBlockEntity {
    public CreateTransformerBlockEntity(BlockPos pos, BlockState state) {
        super(CreateCompatibility.CREATE_TRANSFORMER.getBlockEntityType(CreateTransformerBlockEntity.class), pos, state);
    }

    public float getAvailableStress() {
        return this.capacity - this.stress;
    }

    public long getEnergy() {
        return (long) Math.floor(getAvailableStress() * CreateCompatibility.CONVERSION_RATE);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Energy: " + getEnergy()).withStyle(ChatFormatting.GOLD));

        return true;
    }
}

package com.restonic4.logistics.blocks.base;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public interface InvertiblePlacement {
    Property<Direction> getFacingProperty();

    default BlockState applyShiftInversion(BlockPlaceContext context, BlockState state) {
        if (state == null) {
            return null;
        }

        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            Property<Direction> property = getFacingProperty();
            if (state.hasProperty(property)) {
                Direction current = state.getValue(property);
                return state.setValue(property, current.getOpposite());
            }
        }

        return state;
    }
}

package com.restonic4.logistics.blocks.redstone_reader;

import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.blocks.base.InvertiblePlacement;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * The front ({@link #FACING}) face is the redstone reader and is intentionally NOT a network face;
 * the other five faces are normal cable faces, so the block bridges the energy network like a cable
 * while sensing redstone on its front. Placed facing the player (shift to invert), full 6-axis facing
 * like {@code AccessorBlock}.
 */
public class RedstoneReaderBlock extends BaseNetworkBlock implements InvertiblePlacement {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public RedstoneReaderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public Property<Direction> getFacingProperty() {
        return FACING;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
        return applyShiftInversion(context, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public Set<Direction> getAllowedConnections(BlockState state) {
        Set<Direction> connections = EnumSet.allOf(Direction.class);
        connections.remove(state.getValue(FACING));
        return connections;
    }
}

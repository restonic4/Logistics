package com.restonic4.logistics.blocks.network_connector;

import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class NetworkConnectorBlock extends BaseNetworkBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public NetworkConnectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    private static final VoxelShape SHAPE_NS = Block.box(3.0, 3.0, 3.0, 13.0, 13.0, 13.0);
    private static final VoxelShape SHAPE_EW = Block.box(3.0, 3.0, 3.0, 13.0, 13.0, 13.0);
    private static final VoxelShape SHAPE_UD = Block.box(3.0, 3.0, 3.0, 13.0, 13.0, 13.0);

    private static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case NORTH, SOUTH -> SHAPE_NS;
            case EAST, WEST   -> SHAPE_EW;
            case UP, DOWN     -> SHAPE_UD;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }
}

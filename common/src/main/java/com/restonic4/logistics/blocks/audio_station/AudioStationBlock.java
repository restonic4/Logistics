package com.restonic4.logistics.blocks.audio_station;

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

import static com.restonic4.logistics.utils.MinecraftUtils.getRelativeDown;
import static com.restonic4.logistics.utils.MinecraftUtils.getRelativeRight;

public class AudioStationBlock extends BaseNetworkBlock implements InvertiblePlacement {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public AudioStationBlock(Properties properties) {
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
        BlockState state = this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
        return applyShiftInversion(context, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public Set<Direction> getAllowedConnections(BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction right = getRelativeRight(facing);

        return EnumSet.of(
                facing.getOpposite(),
                right,
                right.getOpposite(),
                getRelativeDown(facing),
                getRelativeDown(facing).getOpposite()
        );
    }

    @Override
    public Property<Direction> getFacingProperty() {
        return FACING;
    }
}

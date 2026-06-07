package com.restonic4.logistics.blocks.accersor;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.blocks.base.InvertiblePlacement;
import com.restonic4.logistics.events.ChunkEvents;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

public class AccessorBlock extends BaseNetworkBlock implements InvertiblePlacement {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public AccessorBlock(Properties properties) {
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

    // TODO: Improve this dirty mess
    public static void registerEvents() {
        ChunkEvents.LOAD.register((level, levelChunk) -> {
            ChunkPos chunkPos = levelChunk.getPos();
            NetworkManager manager = NetworkManager.get(level);
            manager.getAllNetworks().forEach(network ->
                    network.getNodeIndex().getAllNodes().forEach(node -> {
                        if (!(node instanceof AccessorNode accessorNode)) return;

                        BlockPos target = accessorNode.resolveTargetPos();
                        if (target == null) return;
                        if ((target.getX() >> 4) != chunkPos.x || (target.getZ() >> 4) != chunkPos.z) return;

                        accessorNode.onTargetChunkLoaded(level, levelChunk);
                    })
            );
        });

        ChunkEvents.UNLOAD.register((level, levelChunk) -> {
            ChunkPos chunkPos = levelChunk.getPos();
            NetworkManager manager = NetworkManager.get(level);
            manager.getAllNetworks().forEach(network ->
                    network.getNodeIndex().getAllNodes().forEach(node -> {
                        if (!(node instanceof AccessorNode accessorNode)) return;

                        BlockPos target = accessorNode.resolveTargetPos();
                        if (target == null) return;
                        if ((target.getX() >> 4) != chunkPos.x || (target.getZ() >> 4) != chunkPos.z) return;

                        accessorNode.onTargetChunkUnloading(level, levelChunk);
                    })
            );
        });
    }

    private static final VoxelShape SHAPE_NS = Block.box(2.0, 2.0, 0.0, 14.0, 14.0, 16.0);
    private static final VoxelShape SHAPE_EW = Block.box(0.0, 2.0, 2.0, 16.0, 14.0, 14.0);
    private static final VoxelShape SHAPE_UD = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

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

    @Override
    public Set<Direction> getAllowedConnections(BlockState state) {
        Direction facing = state.getValue(FACING);
        return EnumSet.of(facing.getOpposite());
    }
}

package com.restonic4.logistics.blocks.accersor;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.events.ChunkEvents;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class AccessorBlock extends BaseNetworkBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public AccessorBlock(Properties properties) {
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
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void onNodeCreated(NetworkNode node, ServerLevel level, BlockPos pos) {
        if (node instanceof AccessorNode accessorNode) {
            BlockState state = level.getBlockState(pos);
            accessorNode.setFacing(state.getValue(FACING));
        }
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
}

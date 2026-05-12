package com.restonic4.logistics.blocks.base;

import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.nodes.FacingNode;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public interface NetworkBlock {
    NodeTypeRegistry.NetworkNodeType<?> getNodeType();

    default void onNodeCreated(NetworkNode node, ServerLevel level, BlockPos pos) {
        if (node instanceof FacingNode facingNode) {
            BlockState state = level.getBlockState(pos);
            facingNode.setFacing(state.getValue(BlockStateProperties.FACING));
        }
    }
}

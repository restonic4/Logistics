package com.restonic4.logistics.blocks.base;

import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.nodes.FacingNode;
import com.restonic4.logistics.registry.NetworkTypeRegistry;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.EnumSet;
import java.util.Set;

public interface NetworkBlock {
    NodeTypeRegistry.NetworkNodeType<?> getNodeType();

    default ResourceLocation getNetworkTypeID() {
        NodeTypeRegistry.NetworkNodeType<?> nodeType = getNodeType();
        return NetworkTypeRegistry.get(nodeType.networkType());
    }

    default Set<Direction> getAllowedConnections(BlockState state) {
        return EnumSet.allOf(Direction.class);
    }

    default boolean canConnectOnSide(BlockState state, Direction side) {
        return getAllowedConnections(state).contains(side);
    }

    default void onNodeCreated(NetworkNode node, ServerLevel level, BlockPos pos) {
        if (node instanceof FacingNode facingNode) {
            BlockState state = level.getBlockState(pos);
            facingNode.setFacing(state.getValue(BlockStateProperties.FACING));
        }
    }

    default void onNodeRemoved(NetworkNode node, ServerLevel level, BlockPos pos) {

    }
}

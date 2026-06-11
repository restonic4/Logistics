package com.restonic4.logistics.blocks.base;

import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.client.ClientNetworkManager;
import com.restonic4.logistics.networks.nodes.FacingNode;
import com.restonic4.logistics.registry.NetworkTypeRegistry;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import com.restonic4.logistics.screens.RenameNodeScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

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

    default InteractionResult onRightClick(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        NetworkNode node = null;
        if (level.isClientSide()) {
            Network network = ClientNetworkManager.getNetwork(level.dimension(), pos);
            if (network != null) {
                node = network.getNodeIndex().findByBlockPos(pos);
            }
        } else {
            node = NetworkManager.get((ServerLevel) level).getNodeByBlockPos(pos);
        }

        if (node instanceof NameIdentifier nameIdentifierNode) {
            if (level.isClientSide()) {
                RenameNodeScreen.open(node.getBlockPos(), nameIdentifierNode.getName());
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.PASS;
    }
}

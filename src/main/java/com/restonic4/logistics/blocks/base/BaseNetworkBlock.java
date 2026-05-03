package com.restonic4.logistics.blocks.base;

import com.restonic4.logistics.networks.energy.NetworkManager;
import com.restonic4.logistics.networks.energy.NetworkNode;
import com.restonic4.logistics.networks.energy.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BaseNetworkBlock extends Block implements NetworkBlock {
    private NodeTypeRegistry.NetworkNodeType<?> nodeType;

    public BaseNetworkBlock(Properties properties) {
        super(properties);
    }

    public void setNodeType(NodeTypeRegistry.NetworkNodeType<?> nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    public NodeTypeRegistry.NetworkNodeType<?> getNodeType() {
        return nodeType;
    }

    @Override
    public void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState oldBlockState, boolean isMoving) {
        super.onPlace(blockState, level, blockPos, oldBlockState, isMoving);

        if (blockState.is(oldBlockState.getBlock())) return;

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            BlockPos immutableBlockPos = blockPos.immutable(); // This is necessary because otherwise it brutally implodes with /fill commands

            NetworkNode newNode = getNodeType().create(immutableBlockPos);
            onNodeCreated(newNode, serverLevel, immutableBlockPos);
            NetworkManager.get(serverLevel).onMemberPlaced(newNode);
        }
    }

    @Override
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState newBlockState, boolean isMoving) {
        if (!blockState.is(newBlockState.getBlock())) {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                BlockPos immutableBlockPos = blockPos.immutable();
                NetworkManager.get(serverLevel).onMemberRemoved(immutableBlockPos);
            }
        }

        super.onRemove(blockState, level, blockPos, newBlockState, isMoving);
    }
}

package com.restonic4.logistics.compatibility.create.blocks.transformer;

import com.restonic4.logistics.blocks.base.NetworkBlock;
import com.restonic4.logistics.compatibility.create.CreateCompatibility;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CreateTransformerBlock extends DirectionalKineticBlock implements NetworkBlock, IBE<CreateTransformerBlockEntity> {
    public CreateTransformerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Class<CreateTransformerBlockEntity> getBlockEntityClass() {
        return CreateTransformerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CreateTransformerBlockEntity> getBlockEntityType() {
        return CreateCompatibility.CREATE_TRANSFORMER.getBlockEntityType(CreateTransformerBlockEntity.class);
    }

    @Override
    public NodeTypeRegistry.NetworkNodeType<?> getNodeType() {
        return CreateCompatibility.CREATE_TRANSFORMER.getNodeType();
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

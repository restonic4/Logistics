package com.restonic4.logistics.blocks.base;

import com.restonic4.logistics.energy.EnergyNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class BaseNetworkBlock extends Block implements EntityBlock {
    public BaseNetworkBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState oldBlockState, boolean isMoving) {
        super.onPlace(blockState, level, blockPos, oldBlockState, isMoving);

        if (blockState.is(oldBlockState.getBlock())) return;

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            EnergyNetworkManager.get(serverLevel).onMemberPlaced(serverLevel, blockPos);
        }
    }

    @Override
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState newBlockState, boolean isMoving) {
        if (!blockState.is(newBlockState.getBlock())) {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                EnergyNetworkManager.get(serverLevel).onMemberRemoved(serverLevel, blockPos);
            }
        }

        super.onRemove(blockState, level, blockPos, newBlockState, isMoving);
    }

    @Override
    public boolean triggerEvent(BlockState blockState, Level level, BlockPos blockPos, int i, int j) {
        super.triggerEvent(blockState, level, blockPos, i, j);
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        return blockEntity != null && blockEntity.triggerEvent(i, j);
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState blockState, Level level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        return blockEntity instanceof MenuProvider ? (MenuProvider)blockEntity : null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> blockEntityType,
            BlockEntityType<E> blockEntityType2,
            BlockEntityTicker<? super E> blockEntityTicker
    ) {
        return blockEntityType2 == blockEntityType ? (BlockEntityTicker<A>) blockEntityTicker : null;
    }
}

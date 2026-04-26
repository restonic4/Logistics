package com.restonic4.logistics.blocks.machine;

import com.restonic4.logistics.blocks.BlockEntityRegistry;
import com.restonic4.logistics.blocks.entity.GeneratorBlockEntity;
import com.restonic4.logistics.energy.EnergyNetworkManager;
import com.restonic4.logistics.energy.EnergyNodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A simple always-on energy generator.
 * Produces GENERATOR_PRODUCTION_PER_TICK EU/t constantly.
 * No fuel, no crafting — just a test source.
 */
public class GeneratorBlock extends BaseEntityBlock implements EnergyNodeBlock {

    public GeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            EnergyNetworkManager.get(serverLevel).onMemberPlaced(serverLevel, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                EnergyNetworkManager.get(serverLevel).onMemberRemoved(serverLevel, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Generators don't need their own tick — the network ticks them.
        // But if you add fuel consumption later, you'd tick here.
        return null;
    }
}
package com.restonic4.logistics.blocks.base;

import com.restonic4.logistics.energy.EnergyNetwork;
import com.restonic4.logistics.energy.EnergyNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BaseNetworkBlockEntity extends BlockEntity implements EnergyNode {
    @Nullable private EnergyNetwork network;

    public BaseNetworkBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    public EnergyNetwork getEnergyNetwork() { return network; }

    @Override
    public void setEnergyNetwork(EnergyNetwork network) { this.network = network; }

    @Override
    public BlockPos getEnergyPos() { return getBlockPos(); }
}

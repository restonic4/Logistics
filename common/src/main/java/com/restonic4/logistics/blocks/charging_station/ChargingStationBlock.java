package com.restonic4.logistics.blocks.charging_station;

import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;

public class ChargingStationBlock extends BaseNetworkBlock {
    public ChargingStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            ChargingStationScreenDispatcher.open(pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Network network = NetworkManager.get(serverLevel).getNetworkByBlockPos(pos);
            if (network instanceof EnergyNetwork energyNetwork) {
                NetworkNode node = energyNetwork.getNodeIndex().findByBlockPos(pos);
                if (node instanceof ChargingStationNode station) {
                    ItemStack held = station.getHeldItem();
                    if (!held.isEmpty()) {
                        popResource(level, pos, held);
                    }
                }
            }
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }
}
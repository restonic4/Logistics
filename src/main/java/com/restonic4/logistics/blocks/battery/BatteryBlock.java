package com.restonic4.logistics.blocks.battery;

import com.llamalad7.mixinextras.sugar.Local;
import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.energy.Network;
import com.restonic4.logistics.energy.NetworkManager;
import com.restonic4.logistics.energy.NetworkNode;
import com.restonic4.logistics.energy.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BatteryBlock extends BaseNetworkBlock {
    private final Map<Long, Long> pendingEnergy = new HashMap<>();

    public BatteryBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public void setPendingEnergy(BlockPos pos, long energy) {
        pendingEnergy.put(pos.asLong(), energy);
    }

    @Override
    protected void onNodeCreated(NetworkNode node, ServerLevel level, BlockPos pos) {
        if (node instanceof BatteryNode battery) {
            Long energy = pendingEnergy.remove(pos.asLong());
            if (energy != null) {
                battery.setStoredEnergy(energy);
            }
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            Network network = NetworkManager.get(serverLevel).getNetworkByBlockPos(pos);
            if (network != null) {
                NetworkNode node = network.getNodeRegistry().findByBlockPos(pos);
                if (node instanceof BatteryNode battery) {
                    ItemStack drop = new ItemStack(this);
                    drop.getOrCreateTag().putLong("stored_energy", battery.getStoredEnergy());
                    popResource(level, pos, drop);
                    return;
                }
            }
        }

        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }
}
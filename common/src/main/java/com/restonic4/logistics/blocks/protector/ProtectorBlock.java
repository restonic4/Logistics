package com.restonic4.logistics.blocks.protector;

import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

import java.util.EnumSet;
import java.util.Set;

public class ProtectorBlock extends BaseNetworkBlock {
    public ProtectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onNodeRemoved(NetworkNode node, ServerLevel level, BlockPos pos) {
        ServerProtectionCache.updateAllCachesForLevel(level, "Protector node was removed!");
    }

    @Override
    public Set<Direction> getAllowedConnections(BlockState state) {
        return EnumSet.of(Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
    }
}

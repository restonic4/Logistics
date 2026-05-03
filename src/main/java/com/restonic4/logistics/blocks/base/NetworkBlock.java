package com.restonic4.logistics.blocks.base;

import com.restonic4.logistics.networks.energy.NetworkNode;
import com.restonic4.logistics.networks.energy.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface NetworkBlock {
    NodeTypeRegistry.NetworkNodeType<?> getNodeType();

    default void onNodeCreated(NetworkNode node, ServerLevel level, BlockPos pos) { }
}

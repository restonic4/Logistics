package com.restonic4.logistics.blocks.base;

import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.registries.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface NetworkBlock {
    NodeTypeRegistry.NetworkNodeType<?> getNodeType();

    default void onNodeCreated(NetworkNode node, ServerLevel level, BlockPos pos) { }
}

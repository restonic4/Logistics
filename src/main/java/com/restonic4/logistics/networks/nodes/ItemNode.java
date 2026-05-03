package com.restonic4.logistics.networks.nodes;

import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.core.BlockPos;

public abstract class ItemNode extends NetworkNode {
    public ItemNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }
}

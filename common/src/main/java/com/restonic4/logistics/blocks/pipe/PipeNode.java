package com.restonic4.logistics.blocks.pipe;

import com.restonic4.logistics.networks.nodes.ItemNode;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.core.BlockPos;

public class PipeNode extends ItemNode {
    public PipeNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }
}

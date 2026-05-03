package com.restonic4.logistics.blocks.pipe;

import com.restonic4.logistics.networks.energy.NetworkNode;
import com.restonic4.logistics.networks.energy.NodeTypeRegistry;
import net.minecraft.core.BlockPos;

public class PipeNode extends NetworkNode {
    public PipeNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }
}

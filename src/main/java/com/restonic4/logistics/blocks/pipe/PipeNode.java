package com.restonic4.logistics.blocks.pipe;

import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.registries.NodeTypeRegistry;
import net.minecraft.core.BlockPos;

public class PipeNode extends EnergyNode {
    public PipeNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }
}

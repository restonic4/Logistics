package com.restonic4.logistics.blocks.cable;

import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.flags.NetworkFlag;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.core.BlockPos;

public class CableNode extends EnergyNode {
    public CableNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }
}

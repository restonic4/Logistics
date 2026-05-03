package com.restonic4.logistics.blocks.generator;

import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.registries.NodeTypeRegistry;
import net.minecraft.core.BlockPos;


public class GeneratorNode extends EnergyNode {
    public static final long PRODUCTION_PER_TICK = 20L;

    public GeneratorNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        EnergyNetwork energyNetwork = getNetwork();
        if (energyNetwork == null) return;

        energyNetwork.reportEnergyProduction(PRODUCTION_PER_TICK);
    }
}

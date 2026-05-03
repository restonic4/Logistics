package com.restonic4.logistics.blocks.generator;

import com.restonic4.logistics.networks.energy.Network;
import com.restonic4.logistics.networks.energy.NetworkNode;
import com.restonic4.logistics.networks.energy.NodeTypeRegistry;
import net.minecraft.core.BlockPos;


public class GeneratorNode extends NetworkNode {
    public static final long PRODUCTION_PER_TICK = 20L;

    public GeneratorNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        Network network = getNetwork();
        if (network == null) return;

        network.reportEnergyProduction(PRODUCTION_PER_TICK);
    }
}

package com.restonic4.logistics.blocks.generator;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.energy.Network;
import com.restonic4.logistics.energy.NetworkNode;
import com.restonic4.logistics.energy.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;


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

package com.restonic4.logistics.compatibility.create.blocks.transformer;

import com.restonic4.logistics.compatibility.create.CreateCompatibility;
import com.restonic4.logistics.networks.energy.Network;
import com.restonic4.logistics.networks.energy.NetworkNode;
import com.restonic4.logistics.networks.energy.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class CreateTransformerNode extends NetworkNode {
    public CreateTransformerNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        Network network = getNetwork();
        if (network == null) return;

        ServerLevel level = network.getServerLevel();
        if (!(level.getBlockEntity(getBlockPos()) instanceof CreateTransformerBlockEntity transformer)) return;

        if (transformer.isOverStressed()) return;

        long energy = transformer.getEnergy();
        if (energy == 0) return;

        network.reportEnergyProduction(energy);

        if(energy > 0 && level.getGameTime() % CreateCompatibility.CONVERSION_LOSS_TICKS == 0) {
            network.requestEnergyConsumption(1);
        }
    }
}

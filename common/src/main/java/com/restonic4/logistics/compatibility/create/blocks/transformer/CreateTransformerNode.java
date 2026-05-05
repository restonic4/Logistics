package com.restonic4.logistics.compatibility.create.blocks.transformer;

import com.restonic4.logistics.compatibility.create.CreateCompatibility;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class CreateTransformerNode extends EnergyNode {
    public CreateTransformerNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        EnergyNetwork energyNetwork = getNetwork();
        if (energyNetwork == null) return;

        ServerLevel level = energyNetwork.getServerLevel();
        if (!(level.getBlockEntity(getBlockPos()) instanceof CreateTransformerBlockEntity transformer)) return;

        if (transformer.isOverStressed()) return;

        long energy = transformer.getEnergy();
        if (energy == 0) return;

        energyNetwork.reportEnergyProduction(energy);

        if(energy > 0 && level.getGameTime() % CreateCompatibility.CONVERSION_LOSS_TICKS == 0) {
            energyNetwork.requestEnergyConsumption(1);
        }
    }
}

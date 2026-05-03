package com.restonic4.logistics.compatibility.create.blocks.motor;

import com.restonic4.logistics.compatibility.create.CreateCompatibility;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.registries.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class CreateMotorNode extends EnergyNode {
    public CreateMotorNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        EnergyNetwork energyNetwork = getNetwork();
        if (energyNetwork == null) return;

        ServerLevel level = energyNetwork.getServerLevel();
        if (!(level.getBlockEntity(getBlockPos()) instanceof CreateMotorBlockEntity motor)) return;

        long requiredEnergy = (long) Math.ceil(motor.getTheoreticalStressPerTick() * CreateCompatibility.CONVERSION_RATE);
        if (requiredEnergy == 0) requiredEnergy = 1;

        long consumed = energyNetwork.requestEnergyConsumption(requiredEnergy);
        boolean hasEnoughToRun = consumed >= requiredEnergy;

        if (motor.hasEnoughEnergy() != hasEnoughToRun) {
            motor.setEnergyState(hasEnoughToRun);
        }

        boolean actuallyUsed = hasEnoughToRun && !motor.isOverloaded();
        if (!actuallyUsed && consumed > 0) {
            energyNetwork.reportEnergyProduction(consumed);
        } else if(level.getGameTime() % CreateCompatibility.CONVERSION_LOSS_TICKS == 0) {
            energyNetwork.requestEnergyConsumption(1);
        }
    }
}

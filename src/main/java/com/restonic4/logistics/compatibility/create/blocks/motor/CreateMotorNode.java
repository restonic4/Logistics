package com.restonic4.logistics.compatibility.create.blocks.motor;

import com.restonic4.logistics.compatibility.create.CreateCompatibility;
import com.restonic4.logistics.energy.Network;
import com.restonic4.logistics.energy.NetworkNode;
import com.restonic4.logistics.energy.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class CreateMotorNode extends NetworkNode {
    public CreateMotorNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        Network network = getNetwork();
        if (network == null) return;

        ServerLevel level = network.getServerLevel();
        if (!(level.getBlockEntity(getBlockPos()) instanceof CreateMotorBlockEntity motor)) return;

        long requiredEnergy = (long) Math.ceil(motor.getTheoreticalStressPerTick() * CreateCompatibility.CONVERSION_RATE);
        if (requiredEnergy == 0) requiredEnergy = 1;

        long consumed = network.requestEnergyConsumption(requiredEnergy);
        boolean hasEnoughToRun = consumed >= requiredEnergy;

        if (motor.hasEnoughEnergy() != hasEnoughToRun) {
            motor.setEnergyState(hasEnoughToRun);
        }

        boolean actuallyUsed = hasEnoughToRun && !motor.isOverloaded();
        if (!actuallyUsed && consumed > 0) {
            network.reportEnergyProduction(consumed);
        } else if(level.getGameTime() % CreateCompatibility.CONVERSION_LOSS_TICKS == 0) {
            network.requestEnergyConsumption(1);
        }
    }
}

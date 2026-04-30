package com.restonic4.logistics.compatibility.create.blocks.motor;

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

        if (!(level.getBlockEntity(getBlockPos()) instanceof CreateMotorBlockEntity be)) return;

        float speed = Math.abs(be.getSpeedSetting());
        if (speed == 0) return;

        long consumed = network.requestEnergyConsumption((long) speed);
        boolean hasEnough = consumed >= speed;

        if (be.hasEnoughEnergy != hasEnough) {
            be.hasEnoughEnergy = hasEnough;
            be.updateGeneratedRotation();
            be.setChanged();
            be.sendData();
        }

        if (!hasEnough) {
            network.reportEnergyProduction(consumed);
        }
    }
}

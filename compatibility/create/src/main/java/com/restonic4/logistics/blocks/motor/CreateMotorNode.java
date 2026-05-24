package com.restonic4.logistics.blocks.motor;

import com.restonic4.logistics.CreateCommonCompatibility;
import com.restonic4.logistics.networks.nodes.FacingNode;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CreateMotorNode extends EnergyNode implements FacingNode {
    @Nullable private Direction facing = null;

    public CreateMotorNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        EnergyNetwork energyNetwork = getNetwork();
        if (energyNetwork == null) return;

        ServerLevel level = energyNetwork.getServerLevel();
        if (!(level.getBlockEntity(getBlockPos()) instanceof CreateMotorBlockEntity motor)) return;

        long requiredEnergy = (long) Math.ceil(motor.getTheoreticalStressPerTick() * CreateCommonCompatibility.CONVERSION_RATE);
        if (requiredEnergy == 0) requiredEnergy = 1;

        long consumed = energyNetwork.requestEnergyConsumption(requiredEnergy);
        boolean hasEnoughToRun = consumed >= requiredEnergy;

        if (motor.hasEnoughEnergy() != hasEnoughToRun) {
            motor.setEnergyState(hasEnoughToRun);
        }

        boolean actuallyUsed = hasEnoughToRun && !motor.isOverloaded();
        if (!actuallyUsed && consumed > 0) {
            energyNetwork.reportEnergyProduction(consumed);
        } else if(level.getGameTime() % CreateCommonCompatibility.CONVERSION_LOSS_TICKS == 0) {
            energyNetwork.requestEnergyConsumption(1);
        }
    }

    @Override
    public void setFacing(@NotNull Direction facing) {
        this.onFacingChange(this.facing, facing, this);
        this.facing = facing;
    }

    @Override
    @Nullable public Direction getFacing() {
        return facing;
    }
}

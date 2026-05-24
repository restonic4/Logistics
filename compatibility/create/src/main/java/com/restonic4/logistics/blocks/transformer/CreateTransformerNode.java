package com.restonic4.logistics.blocks.transformer;

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

public class CreateTransformerNode extends EnergyNode implements FacingNode {
    @Nullable private Direction facing = null;

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

        if(energy > 0 && level.getGameTime() % CreateCommonCompatibility.CONVERSION_LOSS_TICKS == 0) {
            energyNetwork.requestEnergyConsumption(1);
        }
    }

    @Override
    public void setFacing(@NotNull Direction facing) {
        this.onFacingChange(this.facing, facing, this);
        this.facing = facing;
    }

    @Override
    @Nullable
    public Direction getFacing() {
        return facing;
    }
}

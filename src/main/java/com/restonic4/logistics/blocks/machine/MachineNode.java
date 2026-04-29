package com.restonic4.logistics.blocks.machine;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.energy.Network;
import com.restonic4.logistics.energy.NetworkNode;
import com.restonic4.logistics.energy.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public class MachineNode extends NetworkNode {
    public static final long CONSUMPTION_PER_TICK = 15L;

    public MachineNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        Network network = getNetwork();
        if (network == null) return;

        network.requestEnergyConsumption(CONSUMPTION_PER_TICK);
    }
}

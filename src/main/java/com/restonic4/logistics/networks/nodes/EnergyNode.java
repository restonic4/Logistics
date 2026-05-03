package com.restonic4.logistics.networks.nodes;

import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import net.minecraft.core.BlockPos;

public abstract class EnergyNode extends NetworkNode {
    public EnergyNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    /**
     * @param amount The energy offered to this node.
     * @param simulate If true, don't actually change the energy level.
     * @return The amount of energy that was NOT accepted (overflow).
     */
    public long receiveEnergy(long amount, boolean simulate) {
        return amount; // Default: doesn't accept energy
    }

    /**
     * @param amount The energy requested from this node.
     * @param simulate If true, don't actually change the energy level.
     * @return The amount of energy actually extracted.
     */
    public long extractEnergy(long amount, boolean simulate) {
        return 0; // Default: doesn't provide energy
    }

    // For nodes that store energy
    public long getStoredEnergy() {
        return 0;
    }
    public long getMaxStorage() {
        return 0;
    }

    public EnergyNetwork getNetwork() {
        if (super.getNetwork() instanceof EnergyNetwork energyNetwork) return energyNetwork;
        return null;
    }
}

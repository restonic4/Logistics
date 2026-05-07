package com.restonic4.logistics.networks.nodes;

import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.flags.NetworkFlag;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;

public abstract class EnergyNode extends NetworkNode  {
    private long storedEnergy = 0;
    private long maxStorage = 0;

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
    public long getStoredEnergy() { return storedEnergy; }
    public long getMaxStorage() { return maxStorage; }
    public void setStoredEnergy(long energy) {
        long newEnergy = Math.min(Math.max(energy, 0), getMaxStorage());
        if (this.storedEnergy != newEnergy) {
            this.storedEnergy = newEnergy;
            markDirty(NetworkFlag.STORAGE_CHANGED);
        }
    }
    public void setMaxStorage(long energy) {
        maxStorage = energy;
        markDirty(NetworkFlag.MAX_STORAGE_CHANGED);
    }

    public EnergyNetwork getNetwork() {
        if (super.getNetwork() instanceof EnergyNetwork energyNetwork) return energyNetwork;
        return null;
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        boolean added = super.buildDebugScannerTooltip(builder, isSneaking);

        Network network = getNetwork();
        if (!(network instanceof EnergyNetwork energyNetwork)) return added;

        builder.spacer();
        builder.keyValue("Stored buffer", energyNetwork.getStoredEnergyBuffer() + "/" + energyNetwork.getTotalEnergyBuffer(), ChatFormatting.YELLOW);
        builder.keyValue("Stored cable buffer", energyNetwork.getStoredCableEnergyBuffer() + "/" + energyNetwork.getTotalCableEnergyBuffer(), ChatFormatting.YELLOW);

        return true;
    }
}

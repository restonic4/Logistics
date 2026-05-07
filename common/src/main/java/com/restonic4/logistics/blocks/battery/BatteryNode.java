package com.restonic4.logistics.blocks.battery;

import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

// TODO: For some reason ti did not save the storage, check this
public class BatteryNode extends EnergyNode {
    public static final long MAX_STORAGE = 10_000L;
    public static final long CHARGE_RATE = 40L;
    public static final long DISCHARGE_RATE = 40L;

    public BatteryNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
        setMaxStorage(MAX_STORAGE);
    }

    @Override
    public long receiveEnergy(long amount, boolean simulate) {
        long canReceive = Math.min(amount, CHARGE_RATE);
        long space = getMaxStorage() - getStoredEnergy();
        long accepted = Math.min(canReceive, space);

        if (!simulate) {
            setStoredEnergy(getStoredEnergy() + accepted);
        }

        return amount - accepted;
    }

    @Override
    public long extractEnergy(long amount, boolean simulate) {
        long canExtract = Math.min(amount, DISCHARGE_RATE);
        long extracted = Math.min(canExtract, getStoredEnergy());

        if (!simulate) {
            setStoredEnergy(getStoredEnergy() - extracted);
        }

        return extracted;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        tag.putLong("stored_energy", getStoredEnergy());
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        setStoredEnergy(tag.getLong("stored_energy"));
    }

    @Override
    public boolean buildScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildScannerTooltip(builder, isSneaking);

        builder.spacer();
        builder.keyValue("Battery storage", String.valueOf(getStoredEnergy()), ChatFormatting.YELLOW);

        return true;
    }
}

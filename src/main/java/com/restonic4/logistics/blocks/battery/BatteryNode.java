package com.restonic4.logistics.blocks.battery;

import com.restonic4.logistics.energy.Network;
import com.restonic4.logistics.energy.NetworkNode;
import com.restonic4.logistics.energy.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class BatteryNode extends NetworkNode {
    public static final long MAX_STORAGE = 10_000L;
    public static final long CHARGE_RATE = 40L;
    public static final long DISCHARGE_RATE = 40L;

    private long storedEnergy = 0;

    public BatteryNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public long receiveEnergy(long amount, boolean simulate) {
        long canReceive = Math.min(amount, CHARGE_RATE);
        long space = MAX_STORAGE - storedEnergy;
        long accepted = Math.min(canReceive, space);

        if (!simulate) {
            storedEnergy += accepted;
        }

        return amount - accepted;
    }

    @Override
    public long extractEnergy(long amount, boolean simulate) {
        long canExtract = Math.min(amount, DISCHARGE_RATE);
        long extracted = Math.min(canExtract, storedEnergy);

        if (!simulate) {
            storedEnergy -= extracted;
        }

        return extracted;
    }

    @Override
    public long getMaxStorage() {
        return MAX_STORAGE;
    }

    @Override
    public long getStoredEnergy() {
        return storedEnergy;
    }

    public void setStoredEnergy(long energy) {
        this.storedEnergy = Math.min(Math.max(energy, 0), MAX_STORAGE);
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        tag.putLong("stored_energy", storedEnergy);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        this.storedEnergy = tag.getLong("stored_energy");
    }
}

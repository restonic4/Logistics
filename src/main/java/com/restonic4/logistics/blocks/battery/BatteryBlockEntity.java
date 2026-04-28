package com.restonic4.logistics.blocks.battery;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.base.BaseNetworkBlockEntity;
import com.restonic4.logistics.energy.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores up to 10,000 EU.
 * Charges at up to 40 EU/t, discharges at up to 40 EU/t.
 *
 * The network will charge it when there's surplus and discharge it
 * when producers can't meet demand — automatically, because it implements
 * IEnergyStorage (both producer and consumer).
 */
public class BatteryBlockEntity extends BaseNetworkBlockEntity implements EnergyStorage {
    public static final long MAX_STORAGE     = 10_000L;
    public static final long CHARGE_RATE     = 40L;   // EU/t max charge
    public static final long DISCHARGE_RATE  = 40L;   // EU/t max discharge

    private long stored = 0;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(BlockRegistry.BATTERY_BLOCK.getBlockEntityType(), pos, state);
    }

    // -------------------------------------------------------------------------
    // IEnergyStorage (IEnergyProducer side — discharging)
    // -------------------------------------------------------------------------

    @Override
    public long produceEnergy(long budgetAvailable) {
        long canProvide = Math.min(DISCHARGE_RATE, stored);
        long actual = Math.min(canProvide, budgetAvailable);
        stored -= actual;
        this.setChanged();
        return actual;
    }

    @Override
    public long consumeEnergy(long offered) {
        long canAccept = Math.min(CHARGE_RATE, MAX_STORAGE - stored);
        long actual = Math.min(canAccept, offered);
        stored += actual;
        this.setChanged();
        return actual;
    }

    @Override
    public long getMaxConsumptionPerTick() {
        return CHARGE_RATE;
    }

    @Override
    public OfflineEnergyProfile getOfflineConsumerProfile() {
        return OfflineEnergyProfile.stable(CHARGE_RATE);
    }

    @Override
    public OfflineEnergyProfile getOfflineProducerProfile() {
        return OfflineEnergyProfile.stable(DISCHARGE_RATE);
    }

    @Override
    public boolean needsEnergy() { return stored < MAX_STORAGE; }

    // -------------------------------------------------------------------------
    // IEnergyStorage accessors
    // -------------------------------------------------------------------------

    @Override
    public long getStoredEnergy()    { return stored; }

    @Override
    public long getMaxStoredEnergy() { return MAX_STORAGE; }

    @Override
    public boolean canCharge()    { return stored < MAX_STORAGE; }

    @Override
    public boolean canDischarge() { return stored > 0; }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("stored", stored);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stored = tag.getLong("stored");
    }
}
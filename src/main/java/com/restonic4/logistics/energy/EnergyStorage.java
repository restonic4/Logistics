package com.restonic4.logistics.energy;

public interface EnergyStorage extends EnergyProducer, EnergyConsumer {
    long getStoredEnergy();
    long getMaxStoredEnergy();

    default float getChargePercent() {
        if (getMaxStoredEnergy() == 0) return 0f;
        return (float) getStoredEnergy() / getMaxStoredEnergy();
    }

    default boolean canCharge() { return true; }
    default boolean canDischarge() { return true; }
}
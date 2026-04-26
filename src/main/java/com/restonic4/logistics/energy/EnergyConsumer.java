package com.restonic4.logistics.energy;

public interface EnergyConsumer extends EnergyNode {
    long consumeEnergy(long offered);
    long getMaxConsumptionPerTick();
    default boolean needsEnergy() { return true; }
    OfflineEnergyProfile getOfflineConsumerProfile();
}
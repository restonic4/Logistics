package com.restonic4.logistics.energy;

public interface EnergyProducer extends EnergyNode {
    long produceEnergy(long budgetAvailable);
    OfflineEnergyProfile getOfflineProducerProfile();
}
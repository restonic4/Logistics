package com.restonic4.logistics.energy;

@FunctionalInterface
public interface OfflineSimulator {
    long simulate(EnergyNode node, long ticksElapsed, long energyAvailable);
}
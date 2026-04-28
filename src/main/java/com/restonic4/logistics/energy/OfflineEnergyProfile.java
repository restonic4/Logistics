package com.restonic4.logistics.energy;

import java.util.Objects;

public final class OfflineEnergyProfile {
    public enum Mode {
        STABLE,
        CUSTOM
    }

    private final Mode mode;
    private final long ratePerTick; // used by STABLE
    private final OfflineSimulator simulator; // used by CUSTOM

    private OfflineEnergyProfile(Mode mode, long ratePerTick, OfflineSimulator simulator) {
        if (ratePerTick < 0) { throw new IllegalArgumentException("ratePerTick must be >= 0, got: " + ratePerTick); }
        if (mode == Mode.CUSTOM) { Objects.requireNonNull(simulator, "CUSTOM mode requires a non-null OfflineSimulator"); }

        this.mode = mode;
        this.ratePerTick = ratePerTick;
        this.simulator = simulator;
    }

    public static OfflineEnergyProfile stable(long ratePerTick) {
        return new OfflineEnergyProfile(Mode.STABLE, ratePerTick, null);
    }

    public static OfflineEnergyProfile custom(OfflineSimulator simulator) {
        return new OfflineEnergyProfile(Mode.CUSTOM, 0, simulator);
    }

    public long computeOfflineDelta(EnergyNode node, long ticksElapsed, long energyAvailable) {
        return switch (mode) {
            case STABLE -> ratePerTick * ticksElapsed;
            case CUSTOM -> Math.max(0L, simulator.simulate(node, ticksElapsed, energyAvailable));
        };
    }

    public boolean isStable() { return mode == Mode.STABLE; }

    public Mode getMode() { return mode; }

    public long getRatePerTick() { return ratePerTick; }
}

/*
OfflineEnergyProfile.custom(
    (node, ticks, available) -> {
        long energyPerItem = 200;
        long itemsProcessable = Math.min(inventory.count(), available / energyPerItem);
        long consumed = itemsProcessable * energyPerItem;
        inventory.shrink((int) itemsProcessable); // update real state
        processedOffline += itemsProcessable;
        return consumed;
    }
);
 */
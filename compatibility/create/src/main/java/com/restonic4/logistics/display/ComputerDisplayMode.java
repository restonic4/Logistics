package com.restonic4.logistics.display;

public enum ComputerDisplayMode {
    STORED_ENERGY("Stored Energy"),
    TOTAL_ENERGY("Total Capacity"),
    NODE_COUNT("Network Nodes"),
    PRODUCTION("Production"),
    CONSUMPTION("Consumption"),
    NET_ENERGY("Net Energy"),
    TIME_LEFT("Time Left");

    private final String displayName;

    ComputerDisplayMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ComputerDisplayMode byIndex(int index) {
        ComputerDisplayMode[] modes = values();
        if (index < 0 || index >= modes.length) return STORED_ENERGY;
        return modes[index];
    }
}
package com.restonic4.logistics.networks.tooltip;

public interface ScannerTooltipProvider {
    boolean buildNetworkTooltip(TooltipBuilder builder, boolean isSneaking, boolean isDebug);
}

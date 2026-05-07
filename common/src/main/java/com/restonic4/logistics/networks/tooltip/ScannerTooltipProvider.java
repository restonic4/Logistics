package com.restonic4.logistics.networks.tooltip;

public interface ScannerTooltipProvider {
    boolean buildScannerTooltip(TooltipBuilder builder, boolean isSneaking);
    boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking);
}

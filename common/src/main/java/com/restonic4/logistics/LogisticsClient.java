package com.restonic4.logistics;

import com.restonic4.logistics.blocks.computer.ComputerSyncPacket;
import com.restonic4.logistics.compatibility.CompatibilityManager;
import com.restonic4.logistics.events.RenderCallbacks;
import com.restonic4.logistics.networking.NetworkingRegistry;
import com.restonic4.logistics.networks.tooltip.NetworkTooltipPacket;
import com.restonic4.logistics.rendering.NetworkDebugRenderer;
import com.restonic4.logistics.screens.NetworkScannerOverlay;

public class LogisticsClient {
    public static void init() {
        CompatibilityManager.registerClient();
        NetworkScannerOverlay.register();

        RenderCallbacks.ON_LEVEL_RENDERED.register(NetworkDebugRenderer::render);

        NetworkingRegistry.registerClientTargetedPacket(NetworkTooltipPacket.ID, NetworkTooltipPacket::new);
        NetworkingRegistry.registerClientTargetedPacket(ComputerSyncPacket.ID, ComputerSyncPacket::new);
    }
}

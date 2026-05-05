package com.restonic4.logistics;

import com.restonic4.logistics.compatibility.CompatibilityManager;
import com.restonic4.logistics.events.RenderCallbacks;
import com.restonic4.logistics.networking.NetworkTooltipPayload;
import com.restonic4.logistics.rendering.NetworkDebugRenderer;
import com.restonic4.logistics.screens.NetworkScannerOverlay;

public class LogisticsClient {
    public static void init() {
        CompatibilityManager.registerClient();
        NetworkTooltipPayload.registerClient();
        NetworkScannerOverlay.register();

        RenderCallbacks.ON_LEVEL_RENDERED.register(NetworkDebugRenderer::render);
    }
}

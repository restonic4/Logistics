package com.restonic4.logistics;

import com.restonic4.logistics.compatibility.CompatibilityManager;
import com.restonic4.logistics.networking.NetworkTooltipPayload;
import com.restonic4.logistics.rendering.NetworkDebugRenderer;
import com.restonic4.logistics.screens.NetworkScannerOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class LogisticsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CompatibilityManager.registerClient();
        NetworkTooltipPayload.registerClient();
        NetworkScannerOverlay.register();

        WorldRenderEvents.LAST.register((worldRenderContext) -> {
            NetworkDebugRenderer.render(worldRenderContext.matrixStack(), worldRenderContext.camera());
        });
    }
}

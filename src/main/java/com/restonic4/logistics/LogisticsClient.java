package com.restonic4.logistics;

import com.restonic4.logistics.compatibility.CompatibilityManager;
import com.restonic4.logistics.rendering.NetworkDebugRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class LogisticsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CompatibilityManager.registerClient();

        WorldRenderEvents.LAST.register((worldRenderContext) -> {
            NetworkDebugRenderer.render(worldRenderContext.matrixStack(), worldRenderContext.camera());
        });
    }
}

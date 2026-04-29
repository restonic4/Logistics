package com.restonic4.logistics;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class LogisticsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WorldRenderEvents.LAST.register((worldRenderContext) -> {
            if (EnergyNetworkDebugRenderer.isAllowed()) {
                EnergyNetworkDebugRenderer.render(worldRenderContext.matrixStack(), worldRenderContext.camera());
            }
        });
    }
}

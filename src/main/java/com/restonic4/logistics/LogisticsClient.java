package com.restonic4.logistics;

import com.mojang.blaze3d.platform.InputConstants;
import com.restonic4.logistics.screens.EnergyDebugScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class LogisticsClient implements ClientModInitializer {
    private static KeyMapping debugKey;

    @Override
    public void onInitializeClient() {
        debugKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.logistics.energy_debug",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "key.category.logistics"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (debugKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new EnergyDebugScreen());
                }
            }
        });

        WorldRenderEvents.LAST.register((worldRenderContext) -> {
            if (EnergyNetworkDebugRenderer.isAllowed()) {
                EnergyNetworkDebugRenderer.render(worldRenderContext.matrixStack(), worldRenderContext.camera());
            }
        });
    }
}

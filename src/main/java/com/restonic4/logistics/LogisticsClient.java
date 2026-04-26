package com.restonic4.logistics;

import com.mojang.blaze3d.platform.InputConstants;
import com.restonic4.logistics.blocks.BlockRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.RenderType;
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
    }
}

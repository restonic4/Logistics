package com.restonic4.logistics.experiment;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.platform.Services;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class TestScreenCommand {
    private static Screen screenToOpen = null;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (screenToOpen != null) {
                client.setScreen(screenToOpen);
                screenToOpen = null;
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("test_screen")
                            .requires(TestScreenCommand::isAuthorized)
                            .executes(context -> {
                                FabricClientCommandSource source = context.getSource();

                                try {
                                    screenToOpen = new TestScreen();

                                    source.sendFeedback(Component.literal("§aScreen!"));
                                    return 1;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    source.sendError(Component.literal("§cError: " + e.getClass().getSimpleName() + " - " + e.getMessage()));
                                    return 0;
                                }
                            })
            );
        });
    }

    public static boolean isAuthorized(FabricClientCommandSource source) {
        if (Services.PLATFORM.isDevelopmentEnvironment()) return true;

        LocalPlayer player = source.getPlayer();
        if (player == null) return false;

        String playerName = player.getName().getString();
        return "restonic4".equals(playerName) || "Rayelus".equals(playerName);
    }
}
package com.restonic4.logistics.experiment;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.networks.client.ClientNetworkManager;
import com.restonic4.logistics.platform.Services;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class DumpCommand {
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("dump_logistics")
                            .requires(DumpCommand::isAuthorized)
                            .executes(context -> {
                                FabricClientCommandSource source = context.getSource();

                                try {
                                    ClientNetworkManager.dump();

                                    source.sendFeedback(Component.literal("§aUse debug mode on IDE to check things!"));
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
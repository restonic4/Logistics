package com.restonic4.logistics.experiment;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.platform.FabricPlatformHelper;
import com.restonic4.logistics.platform.Services;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ShockwaveCommand {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("shockwave")
                            .requires(ShockwaveCommand::isAuthorized)
                            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                    .then(Commands.argument("maxRadius", DoubleArgumentType.doubleArg(0.1, 10000.0))
                                            .then(Commands.argument("thickness", DoubleArgumentType.doubleArg(0.1, 20.0))
                                                    .then(Commands.argument("expansionDuration", DoubleArgumentType.doubleArg(0.01, 60.0))
                                                            .then(Commands.argument("fadeOutDuration", DoubleArgumentType.doubleArg(0.0, 60.0))
                                                                    .then(Commands.argument("colorHex", IntegerArgumentType.integer())
                                                                            .executes(context -> {
                                                                                CommandSourceStack source = context.getSource();

                                                                                try {
                                                                                    // Use the proper API instead of context.getArgument("pos", Coordinates.class)
                                                                                    BlockPos origin = BlockPosArgument.getBlockPos(context, "pos");

                                                                                    double maxRadius = DoubleArgumentType.getDouble(context, "maxRadius");
                                                                                    double thickness = DoubleArgumentType.getDouble(context, "thickness");
                                                                                    double expansionDuration = DoubleArgumentType.getDouble(context, "expansionDuration");
                                                                                    double fadeOutDuration = DoubleArgumentType.getDouble(context, "fadeOutDuration");
                                                                                    int colorARGB = IntegerArgumentType.getInteger(context, "colorHex");

                                                                                    if ((colorARGB & 0xFF000000) == 0) {
                                                                                        colorARGB |= 0xFF000000;
                                                                                    }

                                                                                    ServerLevel level = source.getLevel();
                                                                                    ServerNetworking.sendToAllInLevel(level, new ShockwavePacket(origin, maxRadius, thickness, expansionDuration, fadeOutDuration, colorARGB));

                                                                                    source.sendSuccess(() -> Component.literal("§aShockwave sent!"), false);
                                                                                    return 1;
                                                                                } catch (Exception e) {
                                                                                    // Log the REAL error to console so you can see what's actually breaking
                                                                                    e.printStackTrace();
                                                                                    source.sendFailure(Component.literal("§cError: " + e.getClass().getSimpleName() + " - " + e.getMessage()));
                                                                                    return 0;
                                                                                }
                                                                            }))))))
                            ));
        });
    }

    public static boolean isAuthorized(CommandSourceStack source) {
        if (Services.PLATFORM.isDevelopmentEnvironment()) return true;

        ServerPlayer player = source.getPlayer();
        if (player == null) return false;

        String playerName = player.getName().getString();
        return "restonic4".equals(playerName) || "Rayelus".equals(playerName);
    }
}
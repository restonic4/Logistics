package com.restonic4.logistics;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side, per-player debug state. The {@code debug_logistics} command is a client command, so on a
 * dedicated server it can only flip the client's {@link Constants#isDebug()} flag (used by the renderer and
 * client logging). The server-side scanner tooltip handler runs in the server JVM, where that flag is never
 * set, so debug info has to be tracked per player here and toggled via a C2S packet.
 */
public class DebugState {
    private static final Set<UUID> DEBUG_PLAYERS = ConcurrentHashMap.newKeySet();

    public static boolean isDebug(ServerPlayer player) {
        return DEBUG_PLAYERS.contains(player.getUUID());
    }

    public static void setDebug(ServerPlayer player, boolean enabled) {
        if (enabled) {
            DEBUG_PLAYERS.add(player.getUUID());
        } else {
            DEBUG_PLAYERS.remove(player.getUUID());
        }
    }
}

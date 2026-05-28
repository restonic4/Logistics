package com.restonic4.logistics.events;

import com.restonic4.logistics.events.core.Event;
import com.restonic4.logistics.events.core.EventFactory;
import com.restonic4.logistics.events.core.EventResult;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class PlayerEvents {
    @FunctionalInterface
    public interface PlayerJoin { EventResult onEvent(MinecraftServer server, ServerPlayer player);}
    public static final Event<PlayerJoin> JOIN = EventFactory.createCancellable(PlayerJoin.class, callbacks -> (server, player) -> {
        for (PlayerJoin callback : callbacks) {
            EventResult result = callback.onEvent(server, player);
            if (result == EventResult.CANCEL) {
                return EventResult.CANCEL;
            }
        }
        return EventResult.PASS;
    });
}

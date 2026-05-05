package com.restonic4.logistics.events;

import com.restonic4.logistics.events.core.Event;
import com.restonic4.logistics.events.core.EventFactory;
import net.minecraft.server.MinecraftServer;

public class ServerTickEvents {
    @FunctionalInterface public interface Start { void onEvent(MinecraftServer server); }
    public static final Event<Start> START = EventFactory.createVoid(Start.class, callbacks -> (server) -> {
        for (Start callback : callbacks) {
            callback.onEvent(server);
        }
    });

    @FunctionalInterface public interface End { void onEvent(MinecraftServer server); }
    public static final Event<End> END = EventFactory.createVoid(End.class, callbacks -> (server) -> {
        for (End callback : callbacks) {
            callback.onEvent(server);
        }
    });
}


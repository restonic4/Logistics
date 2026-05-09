package com.restonic4.logistics.events;

import com.restonic4.logistics.events.core.Event;
import com.restonic4.logistics.events.core.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public class ChunkEvents {
    @FunctionalInterface public interface Load { void onEvent(ServerLevel level, ChunkPos chunkPos); }
    public static final Event<Load> LOAD = EventFactory.createVoid(Load.class, callbacks -> (level, chunkPos) -> {
        for (Load callback : callbacks) {
            callback.onEvent(level, chunkPos);
        }
    });

    @FunctionalInterface public interface Unload { void onEvent(ServerLevel level, ChunkPos chunkPos); }
    public static final Event<Unload> UNLOAD = EventFactory.createVoid(Unload.class, callbacks -> (level, chunkPos) -> {
        for (Unload callback : callbacks) {
            callback.onEvent(level, chunkPos);
        }
    });
}

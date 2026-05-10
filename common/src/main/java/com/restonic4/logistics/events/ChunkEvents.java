package com.restonic4.logistics.events;

import com.restonic4.logistics.events.core.Event;
import com.restonic4.logistics.events.core.EventFactory;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

public class ChunkEvents {
    @FunctionalInterface public interface Load { void onEvent(ServerLevel level, LevelChunk levelChunk); }
    public static final Event<Load> LOAD = EventFactory.createVoid(Load.class, callbacks -> (level, levelChunk) -> {
        for (Load callback : callbacks) {
            callback.onEvent(level, levelChunk);
        }
    });

    @FunctionalInterface public interface Unload { void onEvent(ServerLevel level, LevelChunk levelChunk); }
    public static final Event<Unload> UNLOAD = EventFactory.createVoid(Unload.class, callbacks -> (level, levelChunk) -> {
        for (Unload callback : callbacks) {
            callback.onEvent(level, levelChunk);
        }
    });
}

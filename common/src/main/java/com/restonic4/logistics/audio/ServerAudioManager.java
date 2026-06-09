package com.restonic4.logistics.audio;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerAudioManager {
    private static final Map<UUID, ServerAudioSource> SOURCES = new ConcurrentHashMap<>();

    public static UUID play(ServerLevel level, BlockPos pos, String filePath,
                            float volume, float pitch, float radius, boolean looping) {
        UUID id = UUID.randomUUID();
        ServerAudioSource source = new ServerAudioSource(id, level, pos, filePath,
                volume, pitch, radius, looping);
        SOURCES.put(id, source);
        source.tick(level, pos);
        return id;
    }

    public static void stop(UUID id) {
        ServerAudioSource source = SOURCES.remove(id);
        if (source != null) source.broadcastStop();
    }

    public static void updateVolume(UUID id, float volume) {
        ServerAudioSource source = SOURCES.get(id);
        if (source != null && !source.isStopped()) {
            source.setVolume(volume);
            source.broadcastUpdate();
        }
    }

    public static void updatePitch(UUID id, float pitch) {
        ServerAudioSource source = SOURCES.get(id);
        if (source != null && !source.isStopped()) {
            source.setPitch(pitch);
            source.broadcastUpdate();
        }
    }

    public static void updateRadius(UUID id, float radius) {
        ServerAudioSource source = SOURCES.get(id);
        if (source != null && !source.isStopped()) {
            source.setRadius(radius);
            source.broadcastUpdate();
        }
    }

    public static void tickSource(UUID id, ServerLevel level, BlockPos pos) {
        ServerAudioSource source = SOURCES.get(id);
        if (source != null) {
            if (source.isStopped()) {
                SOURCES.remove(id);
            } else {
                source.tick(level, pos);
            }
        }
    }

    public static ServerAudioSource getSource(UUID id) {
        return SOURCES.get(id);
    }

    public static void clearLevel(ServerLevel level) {
        Iterator<Map.Entry<UUID, ServerAudioSource>> it = SOURCES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ServerAudioSource> entry = it.next();
            if (entry.getValue().getLevel() == level) {
                entry.getValue().broadcastStop();
                it.remove();
            }
        }
    }
}
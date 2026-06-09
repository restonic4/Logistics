package com.restonic4.logistics.audio;

import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientAudioManager {
    private static final Map<UUID, ClientAudioSource> SOURCES = new ConcurrentHashMap<>();

    public static void play(UUID id, Vec3 pos, String filePath, float volume,
                            float pitch, float radius, long elapsedMs, boolean looping) {
        stop(id);
        ClientAudioSource source = new ClientAudioSource(id, pos, filePath, volume,
                pitch, radius, elapsedMs, looping);
        SOURCES.put(id, source);
        source.play();
    }

    public static void stop(UUID id) {
        ClientAudioSource source = SOURCES.remove(id);
        if (source != null) source.stop();
    }

    public static void update(UUID id, float volume, float pitch, float radius) {
        ClientAudioSource source = SOURCES.get(id);
        if (source != null) source.update(volume, pitch, radius);
    }

    public static void tick() {
        for (ClientAudioSource source : SOURCES.values()) {
            source.tick();
        }
    }

    public static void clear() {
        for (ClientAudioSource source : SOURCES.values()) source.stop();
        SOURCES.clear();
    }
}
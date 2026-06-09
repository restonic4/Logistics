package com.restonic4.logistics.audio;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AudioBufferCache {
    private static final Map<String, AudioBuffer> CACHE = new ConcurrentHashMap<>();

    public static AudioBuffer getOrLoad(String path) {
        return CACHE.computeIfAbsent(path, p -> {
            try {
                return AudioBuffer.load(p);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load audio buffer: " + p, e);
            }
        });
    }

    public static void clear() {
        CACHE.values().forEach(AudioBuffer::discard);
        CACHE.clear();
    }
}
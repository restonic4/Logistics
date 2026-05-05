package com.restonic4.logistics.rendering;

import com.mojang.blaze3d.vertex.VertexBuffer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MeshCache {
    public static final long EXPIRATION_TIME_MS = 10000;
    private final Map<UUID, Entry> cache = new ConcurrentHashMap<>();

    public VertexBuffer get(UUID networkId, int currentHash) {
        Entry entry = cache.get(networkId);
        if (entry != null && entry.nodeHash == currentHash) {
            cache.put(networkId, new Entry(entry.buffer, entry.nodeHash, System.currentTimeMillis()));
            return entry.buffer;
        }
        if (entry != null) entry.buffer.close();
        return null;
    }

    public void put(UUID networkId, VertexBuffer buffer, int currentHash) {
        cache.put(networkId, new Entry(buffer, currentHash, System.currentTimeMillis()));
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(e -> {
            if (now - e.getValue().lastAccess > EXPIRATION_TIME_MS) {
                e.getValue().buffer.close();
                return true;
            }
            return false;
        });
    }

    private record Entry(VertexBuffer buffer, int nodeHash, long lastAccess) {}
}

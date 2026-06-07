package com.restonic4.logistics.voicechat;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WalkieTalkieManager {
    private static final Set<UUID> TRANSMITTING = ConcurrentHashMap.newKeySet();

    public static void startTransmitting(UUID player) {
        TRANSMITTING.add(player);
    }

    public static void stopTransmitting(UUID player) {
        TRANSMITTING.remove(player);
    }

    public static boolean isTransmitting(UUID player) {
        return TRANSMITTING.contains(player);
    }
}

package com.restonic4.logistics.registry;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.registry.builders.SoundBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SoundRegistry {
    private static final Map<ResourceLocation, SoundBuilder.SoundEventData> PENDING = new LinkedHashMap<>();
    private static boolean frozen = false;

    public static void register(ResourceLocation id, SoundBuilder.SoundEventData data) {
        if (frozen) throw new RuntimeException("Sound data registry already frozen!");
        PENDING.put(id, data);
    }

    public static Map<ResourceLocation, SoundBuilder.SoundEventData> getAndFreeze() {
        if (!frozen) {
            Constants.LOG.warn("Freezing sound injections!");
            frozen = true;
        }
        return PENDING;
    }
}
package com.restonic4.logistics.networks.registries;

import com.restonic4.logistics.networks.Network;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class NetworkTypeRegistry {
    private static final Map<ResourceLocation, NetworkType<?>> REGISTRY = new HashMap<>();

    public static <T extends Network> NetworkType<T> register(ResourceLocation id, NetworkFactory<T> factory) {
        NetworkType<T> type = new NetworkType<>(factory);
        REGISTRY.put(id, type);
        return type;
    }

    public static NetworkType<?> get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    public static ResourceLocation get(NetworkType<?> type) {
        for (Map.Entry<ResourceLocation, NetworkType<?>> entry : REGISTRY.entrySet()) {
            if (entry.getValue() == type) {
                return entry.getKey();
            }
        }
        return null;
    }

    public record NetworkType<T extends Network>(NetworkFactory<T> factory) {
        public T create(ServerLevel serverLevel) {
            return factory.create(this, serverLevel);
        }
    }

    @FunctionalInterface
    public interface NetworkFactory<T extends Network> {
        T create(NetworkType<T> type, ServerLevel serverLevel);
    }
}
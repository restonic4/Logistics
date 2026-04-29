package com.restonic4.logistics.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

public final class NodeTypeRegistry {
    private static final Map<ResourceLocation, NetworkNodeType<?>> REGISTRY = new HashMap<>();

    public static <T extends NetworkNode> NetworkNodeType<T> register(ResourceLocation id, NodeFactory<T> factory) {
        NetworkNodeType<T> type = new NetworkNodeType<>(factory);
        REGISTRY.put(id, type);
        return type;
    }

    public static NetworkNodeType<?> get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    public static ResourceLocation get(NetworkNodeType<?> type) {
        for (Map.Entry<ResourceLocation, NetworkNodeType<?>> entry : REGISTRY.entrySet()) {
            if (entry.getValue() == type) {
                return entry.getKey();
            }
        }
        return null;
    }

    public record NetworkNodeType<T extends NetworkNode>(NodeFactory<T> factory) {
        public T create(BlockPos pos) {
            return factory.create(this, pos);
        }
    }

    @FunctionalInterface
    public interface NodeFactory<T extends NetworkNode> {
        T create(NetworkNodeType<T> type, BlockPos pos);
    }
}
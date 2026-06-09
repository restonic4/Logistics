package com.restonic4.logistics.networks.client;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.*;

public class ClientNetworkManager {
    private static final Map<ResourceKey<Level>, Map<UUID, Network>> DIMENSIONAL_NETWORKS = new HashMap<>();

    public static void clear() {
        DIMENSIONAL_NETWORKS.clear();
        Constants.LOG.info("Cleared ClientNetworkManager caches due to world disconnect.");
    }

    public static void putNetwork(ResourceKey<Level> dimension, Network network) {
        DIMENSIONAL_NETWORKS
                .computeIfAbsent(dimension, k -> new HashMap<>())
                .put(network.getUUID(), network);
    }

    public static Network getNetwork(ResourceKey<Level> dimension, UUID uuid) {
        Map<UUID, Network> levelNetworks = DIMENSIONAL_NETWORKS.get(dimension);
        return levelNetworks != null ? levelNetworks.get(uuid) : null;
    }

    public static Map<UUID, Network> getNetworksForLevel(ResourceKey<Level> dimension) {
        return DIMENSIONAL_NETWORKS.getOrDefault(dimension, Map.of());
    }

    public static void removeNetwork(ResourceKey<Level> dimension, UUID uuid) {
        Map<UUID, Network> levelNetworks = DIMENSIONAL_NETWORKS.get(dimension);
        if (levelNetworks != null) {
            levelNetworks.remove(uuid);
            if (levelNetworks.isEmpty()) {
                DIMENSIONAL_NETWORKS.remove(dimension);
            }
        }
    }

    public static Collection<Network> getNetworks(ResourceKey<Level> dimension) {
        Map<UUID, Network> map = DIMENSIONAL_NETWORKS.get(dimension);
        return map != null ? map.values() : Collections.emptyList();
    }

    public static void dump() {
        Constants.LOG.debug("Hello");
        Constants.LOG.debug(String.valueOf(DIMENSIONAL_NETWORKS.size()));
        Constants.LOG.debug("Bye");
    }
}

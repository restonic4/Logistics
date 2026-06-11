package com.restonic4.logistics.networks.client;

import com.mojang.authlib.GameProfile;
import com.restonic4.logistics.Constants;
import com.restonic4.logistics.blocks.audio_station.AudioStationNode;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.utils.MinecraftUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.stream.Collectors;

public class ClientNetworkManager {
    private static final Map<ResourceKey<Level>, Map<UUID, Network>> DIMENSIONAL_NETWORKS = new HashMap<>();
    private static List<String> UPLOADED_SOUNDS = new ArrayList<>();

    public static void clear() {
        DIMENSIONAL_NETWORKS.clear();
        UPLOADED_SOUNDS.clear();
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

    public static Network getNetwork(ResourceKey<Level> dimension, BlockPos pos) {
        Map<UUID, Network> levelNetworks = DIMENSIONAL_NETWORKS.get(dimension);
        if (levelNetworks == null) return null;

        for (Network network : levelNetworks.values()) {
            NetworkNode node = network.getNodeIndex().findByBlockPos(pos);
            if (node != null) {
                return network;
            }
        }

        return null;
    }

    public static  <T extends Network> Optional<T> getAdjacentNetwork(ResourceKey<Level> dimension, BlockPos pos, Class<T> networkClass) {
        Network self = getNetwork(dimension, pos);
        if (networkClass.isInstance(self)) {
            return Optional.of(networkClass.cast(self));
        }

        return MinecraftUtils.findNeighbor(pos, neighborPos -> {
            Network candidate = getNetwork(dimension, neighborPos);
            if (networkClass.isInstance(candidate)) {
                return networkClass.cast(candidate);
            }
            return null;
        });
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

    public static void setUploadedSounds(List<String> newSounds) {
        UPLOADED_SOUNDS = newSounds;
    }

    public static List<String> getUploadedSounds() {
        return UPLOADED_SOUNDS;
    }

    /**
     * Pretty label for an uploaded sound. Stored paths look like {@code <uploaderUuid>/<file>.wav};
     * for display we swap the UUID for the uploader's username when they're known to the client.
     * The returned string is for display only — keep using the raw path as the value.
     */
    public static String getSoundDisplayName(String path) {
        if (path == null || path.isEmpty()) return path;

        String[] parts = path.split("/", 2);
        if (parts.length == 2) {
            try {
                UUID uploader = UUID.fromString(parts[0]);
                ClientPacketListener connection = Minecraft.getInstance().getConnection();
                PlayerInfo info = connection != null ? connection.getPlayerInfo(uploader) : null;
                if (info != null) {
                    return info.getProfile().getName() + "/" + parts[1];
                }
            } catch (IllegalArgumentException ignored) {
                // Prefix isn't a UUID — show the path as is.
            }
        }
        return path; // TODO: Minecraft saves usernames on the usercache.json file at server root on dedicated servers. So we might be able to use this if the player is not online? Maybe? Packet to track this data? Like, only the users who uploaded a sound, not all players, so we have some cache.
    }

    public static List<GameProfile> getGameProfiles() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        List<GameProfile> profiles = Collections.emptyList();
        if (connection != null) {
            profiles = connection.getOnlinePlayers().stream().map(PlayerInfo::getProfile).toList();
        }
        return profiles;
    }
}

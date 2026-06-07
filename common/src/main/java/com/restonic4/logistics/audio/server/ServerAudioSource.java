package com.restonic4.logistics.audio.server;

import com.restonic4.logistics.audio.AudioFormat;
import com.restonic4.logistics.audio.network.*;
import com.restonic4.logistics.networking.S2CPacket;
import com.restonic4.logistics.networking.ServerNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ServerAudioSource {
    private final UUID id;
    private final ServerLevel level;
    private Vec3 position;
    private final ResourceLocation soundId;
    private final String url;
    private final AudioFormat format;
    private final boolean isRadio;
    private float volume;
    private float pitch;
    private float radius;
    private boolean playing = false;
    private long elapsedMs = 0;
    private long lastTickMs = 0;
    private final Set<UUID> knownPlayers = ConcurrentHashMap.newKeySet();
    private int syncCounter = 0;

    public ServerAudioSource(UUID id, ServerLevel level, Vec3 position, ResourceLocation soundId, String url,
                             AudioFormat format, boolean isRadio, float volume, float pitch, float radius) {
        this.id = id;
        this.level = level;
        this.position = position;
        this.soundId = soundId;
        this.url = url;
        this.format = format;
        this.isRadio = isRadio;
        this.volume = volume;
        this.pitch = pitch;
        this.radius = radius;
    }

    public void play() {
        playing = true;
        lastTickMs = System.currentTimeMillis();
        elapsedMs = 0;
    }

    public void stop() {
        playing = false;
        broadcastToKnown(new ClientboundAudioStopPacket(id));
        knownPlayers.clear();
    }

    public void tick() {
        if (playing) {
            long now = System.currentTimeMillis();
            elapsedMs += (now - lastTickMs);
            lastTickMs = now;
        }

        double r2 = radius * radius;
        Set<ServerPlayer> inRange = level.players().stream()
                .filter(p -> p.distanceToSqr(position.x, position.y, position.z) < r2)
                .collect(Collectors.toSet());

        Set<UUID> inRangeIds = inRange.stream().map(ServerPlayer::getUUID).collect(Collectors.toSet());

        for (ServerPlayer player : inRange) {
            if (!knownPlayers.contains(player.getUUID())) {
                sendPlay(player);
                knownPlayers.add(player.getUUID());
            }
        }

        knownPlayers.removeIf(uuid -> !inRangeIds.contains(uuid));

        syncCounter++;
        if (syncCounter >= 100) {
            syncCounter = 0;
            if (!knownPlayers.isEmpty()) {
                ClientboundAudioSyncPacket sync = new ClientboundAudioSyncPacket(id, elapsedMs, playing);
                for (UUID uuid : knownPlayers) {
                    ServerPlayer p = level.getServer().getPlayerList().getPlayer(uuid);
                    if (p != null) ServerNetworking.sendToClient(p, sync);
                }
            }
        }
    }

    private void sendPlay(ServerPlayer player) {
        ClientboundAudioPlayPacket packet = new ClientboundAudioPlayPacket(
                id, position, soundId, url, format, isRadio, volume, pitch, radius, isRadio ? 0 : elapsedMs
        );
        ServerNetworking.sendToClient(player, packet);
    }

    private void broadcastToKnown(S2CPacket packet) {
        for (UUID uuid : knownPlayers) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(uuid);
            if (p != null) ServerNetworking.sendToClient(p, packet);
        }
    }

    public void setVolume(float v) {
        this.volume = v;
        broadcastToKnown(new ClientboundAudioUpdatePacket(id, position, volume, pitch, radius));
    }

    public void setPitch(float p) {
        this.pitch = p;
        broadcastToKnown(new ClientboundAudioUpdatePacket(id, position, volume, pitch, radius));
    }

    public void setRadius(float r) {
        this.radius = r;
        broadcastToKnown(new ClientboundAudioUpdatePacket(id, position, volume, pitch, radius));
    }

    public void setPosition(Vec3 pos) {
        this.position = pos;
        broadcastToKnown(new ClientboundAudioUpdatePacket(id, position, volume, pitch, radius));
    }

    public UUID getId() { return id; }
    public boolean isPlaying() { return playing; }
    public boolean isStopped() { return !playing && knownPlayers.isEmpty(); }
}
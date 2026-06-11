package com.restonic4.logistics.audio;

import com.restonic4.logistics.networking.ServerNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ServerAudioSource {
    private final UUID id;
    private final ServerLevel level;
    private final BlockPos pos;
    private final String filePath;
    private float volume;
    private float pitch;
    private float radius;
    private final boolean looping;
    private long elapsedMs;
    private long lastServerTick;
    private long lastRealTime;
    private final long durationMs;
    private final Set<UUID> knownPlayers = new HashSet<>();
    private boolean stopped;

    public ServerAudioSource(
            UUID id, ServerLevel level, BlockPos pos, String filePath,
            float volume, float pitch, float radius, boolean looping
    ) {
        this.id = id;
        this.level = level;
        this.pos = pos;
        this.filePath = filePath;
        this.volume = volume;
        this.pitch = pitch;
        this.radius = radius;
        this.looping = looping;
        this.elapsedMs = 0;
        this.lastServerTick = -1;
        this.lastRealTime = -1;

        long duration;
        try {
            duration = AudioUtils.getWavDurationMs(filePath);
        } catch (Exception e) {
            System.err.println("Could not determine audio duration for: " + filePath);
            duration = Long.MAX_VALUE;
        }
        this.durationMs = duration;
    }

    public void tick(ServerLevel level, BlockPos pos) {
        if (stopped) return;

        long currentTick = level.getServer().getTickCount();
        long currentRealTime = System.currentTimeMillis();

        if (this.lastServerTick == -1) {
            this.lastServerTick = currentTick;
            this.lastRealTime = currentRealTime;
        } else if (currentTick > this.lastServerTick) {
            long delta = currentRealTime - this.lastRealTime;
            this.elapsedMs += (long) (delta * this.pitch);
            this.lastServerTick = currentTick;
            this.lastRealTime = currentRealTime;
        } else {
            this.lastRealTime = currentRealTime;
        }

        if (!looping && this.elapsedMs >= this.durationMs) {
            stop();
            return;
        }

        if (looping && durationMs > 0) {
            this.elapsedMs = this.elapsedMs % durationMs;
        }

        Vec3 center = Vec3.atCenterOf(pos);
        List<ServerPlayer> players = level.players();
        Set<UUID> currentPlayers = new HashSet<>();

        for (ServerPlayer player : players) {
            double distSq = player.distanceToSqr(center.x, center.y, center.z);
            if (distSq <= this.radius * this.radius) {
                currentPlayers.add(player.getUUID());
                if (!this.knownPlayers.contains(player.getUUID())) {
                    sendSync(player, center);
                }
            }
        }

        for (UUID uuid : new HashSet<>(this.knownPlayers)) {
            if (!currentPlayers.contains(uuid)) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
                if (player != null) sendStop(player);
                this.knownPlayers.remove(uuid);
            }
        }

        this.knownPlayers.addAll(currentPlayers);
    }

    private void sendSync(ServerPlayer player, Vec3 center) {
        long sendElapsed = (looping && durationMs > 0) ? (elapsedMs % durationMs) : elapsedMs;
        AudioPlayS2CPacket packet = new AudioPlayS2CPacket(
                this.id, center, this.filePath, this.volume, this.pitch,
                this.radius, sendElapsed, this.looping
        );
        ServerNetworking.sendToClient(player, packet);
    }

    private void sendStop(ServerPlayer player) {
        ServerNetworking.sendToClient(player, new AudioStopS2CPacket(this.id));
    }

    public void broadcastUpdate() {
        AudioUpdateS2CPacket packet = new AudioUpdateS2CPacket(this.id, this.volume, this.pitch, this.radius);
        for (UUID uuid : this.knownPlayers) {
            ServerPlayer player = this.level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) ServerNetworking.sendToClient(player, packet);
        }
    }

    public void broadcastStop() {
        for (UUID uuid : new HashSet<>(this.knownPlayers)) {
            ServerPlayer player = this.level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) sendStop(player);
        }
        this.knownPlayers.clear();
    }

    public UUID getId() { return id; }
    public ServerLevel getLevel() { return level; }
    public void setVolume(float volume) { this.volume = volume; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public void setRadius(float radius) { this.radius = radius; }
    public void stop() { this.stopped = true; }
    public boolean isStopped() { return stopped; }
}
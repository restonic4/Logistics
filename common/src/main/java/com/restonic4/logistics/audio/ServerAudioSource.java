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

/**
 * Authoritative server-side playback clock for one audio source. Advances deterministically
 * per server tick (50ms × clamped pitch), so it is the single source of truth all clients
 * sync to. Tracks which players are in range, telling them to start/stop, and periodically
 * broadcasts the current offset so clients can correct drift.
 */
public class ServerAudioSource {
    /** How often (server ticks) to broadcast the authoritative offset. 40 ticks = 2 seconds. */
    private static final int SYNC_INTERVAL_TICKS = 40;
    private static final long MS_PER_TICK = 50;

    private final UUID id;
    private final ServerLevel level;
    private final BlockPos pos;
    private final String soundId;
    private final String hash;
    private float volume;
    private float pitch;
    private float radius;
    private final boolean looping;
    private long elapsedMs;
    private long lastServerTick;
    private long lastSyncTick;
    private final long durationMs;
    private final Set<UUID> knownPlayers = new HashSet<>();
    private boolean stopped;

    public ServerAudioSource(
            UUID id, ServerLevel level, BlockPos pos, String soundId, String hash,
            float volume, float pitch, float radius, boolean looping
    ) {
        this.id = id;
        this.level = level;
        this.pos = pos;
        this.soundId = soundId;
        this.hash = hash;
        this.volume = volume;
        this.pitch = pitch;
        this.radius = radius;
        this.looping = looping;
        this.elapsedMs = 0;
        this.lastServerTick = -1;
        this.lastSyncTick = 0;

        long duration;
        try {
            java.io.File file = ServerAudioStorage.getSoundFile(soundId);
            duration = file != null ? AudioUtils.getAudioDurationMs(file.getAbsolutePath()) : Long.MAX_VALUE;
        } catch (Exception e) {
            System.err.println("Could not determine audio duration for: " + soundId);
            duration = Long.MAX_VALUE;
        }
        this.durationMs = duration;
    }

    public void tick(ServerLevel level, BlockPos pos) {
        if (stopped) return;

        long currentTick = level.getServer().getTickCount();

        if (this.lastServerTick == -1) {
            this.lastServerTick = currentTick;
            this.lastSyncTick = currentTick;
        } else if (currentTick > this.lastServerTick) {
            // Advance the clock deterministically by elapsed server ticks, scaled by the
            // playback pitch (clamped to OpenAL's effective [0.5, 2.0] range MC enforces).
            long ticks = currentTick - this.lastServerTick;
            float effectivePitch = Math.max(0.5f, Math.min(2.0f, this.pitch));
            this.elapsedMs += (long) (ticks * MS_PER_TICK * effectivePitch);
            this.lastServerTick = currentTick;
        }

        if (!looping && this.elapsedMs >= this.durationMs) {
            stop();
            return;
        }

        if (looping && durationMs > 0 && durationMs != Long.MAX_VALUE) {
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
                    sendPlay(player, center);
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

        // Periodically push the authoritative offset so clients converge on the server clock.
        if (currentTick - this.lastSyncTick >= SYNC_INTERVAL_TICKS) {
            this.lastSyncTick = currentTick;
            broadcastSync();
        }
    }

    private long sendableElapsed() {
        return (looping && durationMs > 0 && durationMs != Long.MAX_VALUE) ? (elapsedMs % durationMs) : elapsedMs;
    }

    private void sendPlay(ServerPlayer player, Vec3 center) {
        AudioPlayS2CPacket packet = new AudioPlayS2CPacket(
                this.id, center, this.soundId, this.hash, this.volume, this.pitch,
                this.radius, sendableElapsed(), this.looping
        );
        ServerNetworking.sendToClient(player, packet);
    }

    private void sendStop(ServerPlayer player) {
        ServerNetworking.sendToClient(player, new AudioStopS2CPacket(this.id));
    }

    private void broadcastSync() {
        long elapsed = sendableElapsed();
        AudioSyncS2CPacket packet = new AudioSyncS2CPacket(this.id, elapsed);
        for (UUID uuid : this.knownPlayers) {
            ServerPlayer player = this.level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) ServerNetworking.sendToClient(player, packet);
        }
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

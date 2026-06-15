package com.restonic4.logistics.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side registry of playing audio-station sources. Each source is played as a
 * {@link LogisticsSoundInstance} through Minecraft's sound engine (so channels are managed by
 * MC and never leak). Also holds the playback registry the {@code SoundBufferLibraryMixin}
 * reads to supply decoded audio, and the soft-resync logic that keeps clients aligned to the
 * authoritative server clock.
 * <p>
 * All methods run on the client (render) thread.
 */
public class ClientAudioManager {
    /** Restart playback only when drift from the server clock exceeds this (ms). */
    private static final long DRIFT_THRESHOLD_MS = 150;

    private static final Map<UUID, ClientSource> SOURCES = new ConcurrentHashMap<>();

    /** What the mixin needs to build a stream for a given (transformed) sound location. */
    public record Playback(File cacheFile, long startMs, boolean loop) {}

    private static final Map<ResourceLocation, Playback> ACTIVE_STREAMS = new ConcurrentHashMap<>();

    private static class ClientSource {
        final UUID id;
        final Vec3 pos;
        final String soundId;
        final String hash;
        final boolean looping;
        final ResourceLocation baseLocation;
        final ResourceLocation pathLocation;

        float volume;
        float pitch;
        float radius;

        long serverElapsedAtRequest;
        long requestTimeNanos;

        File cacheFile;
        LogisticsSoundInstance instance;
        long baseElapsedMs;   // offset at which the current instance started
        long baseTimeNanos;   // client time the current instance started

        ClientSource(UUID id, Vec3 pos, String soundId, String hash,
                     float volume, float pitch, float radius, boolean looping) {
            this.id = id;
            this.pos = pos;
            this.soundId = soundId;
            this.hash = hash;
            this.volume = volume;
            this.pitch = pitch;
            this.radius = radius;
            this.looping = looping;
            this.baseLocation = new ResourceLocation("logistics", "audio_src/" + id);
            // Mirror Sound#getPath() so the mixin's lookup key matches what the engine requests.
            this.pathLocation = Sound.SOUND_LISTER.idToFile(this.baseLocation);
        }
    }

    public static void play(UUID id, Vec3 pos, String soundId, String hash, float volume,
                            float pitch, float radius, long elapsedMs, boolean looping) {
        stop(id);

        ClientSource source = new ClientSource(id, pos, soundId, hash, volume, pitch, radius, looping);
        source.serverElapsedAtRequest = elapsedMs;
        source.requestTimeNanos = System.nanoTime();
        SOURCES.put(id, source);

        // Ensure the file is cached (downloading from the server if needed), then start. By the
        // time the download finishes we advance the start offset by the elapsed wall time so we
        // still join the server's playback in the right place.
        ClientAudioCache.ensure(soundId, hash, file -> {
            if (SOURCES.get(id) != source) return; // superseded or stopped before the download finished

            source.cacheFile = file;
            long now = System.nanoTime();
            long advanced = (long) ((now - source.requestTimeNanos) / 1_000_000.0 * clampPitch(source.pitch));
            startInstance(source, source.serverElapsedAtRequest + advanced, now);
        });
    }

    public static void stop(UUID id) {
        ClientSource source = SOURCES.remove(id);
        if (source != null) detach(source);
    }

    public static void update(UUID id, float volume, float pitch, float radius) {
        ClientSource source = SOURCES.get(id);
        if (source == null) return;
        source.volume = volume;
        source.pitch = pitch;
        source.radius = radius;
        if (source.instance != null) source.instance.updateConfig(volume, pitch, radius);
    }

    /**
     * Applies the authoritative server offset. If our playback has drifted past the threshold,
     * cleanly restart at the corrected offset (the engine releases and reacquires the channel —
     * no leak). Small drift is left alone to avoid audible restarts.
     */
    public static void resync(UUID id, long serverElapsedMs) {
        ClientSource source = SOURCES.get(id);
        if (source == null || source.instance == null || source.cacheFile == null) return;

        long now = System.nanoTime();
        long clientEst = source.baseElapsedMs
                + (long) ((now - source.baseTimeNanos) / 1_000_000.0 * clampPitch(source.pitch));

        long durationMs = AudioDecoder.getDurationMs(source.cacheFile);
        long diff;
        if (source.looping && durationMs > 0) {
            long clientWrapped = Math.floorMod(clientEst, durationMs);
            long serverWrapped = Math.floorMod(serverElapsedMs, durationMs);
            diff = serverWrapped - clientWrapped;
            if (diff > durationMs / 2) diff -= durationMs;
            if (diff < -durationMs / 2) diff += durationMs;
        } else {
            diff = serverElapsedMs - clientEst;
        }

        if (Math.abs(diff) > DRIFT_THRESHOLD_MS) {
            detach(source);
            startInstance(source, serverElapsedMs, now);
        }
    }

    public static void tick() {
        // Volume/position are pulled by the engine via the tickable instance; nothing to do.
    }

    public static void clear() {
        for (ClientSource source : SOURCES.values()) detach(source);
        SOURCES.clear();
        ACTIVE_STREAMS.clear();
    }

    /** Looked up by SoundBufferLibraryMixin to supply our decoded audio for a sound location. */
    public static Playback getPlayback(ResourceLocation pathLocation) {
        return ACTIVE_STREAMS.get(pathLocation);
    }

    private static void startInstance(ClientSource source, long startMs, long startTimeNanos) {
        source.baseElapsedMs = startMs;
        source.baseTimeNanos = startTimeNanos;
        ACTIVE_STREAMS.put(source.pathLocation, new Playback(source.cacheFile, startMs, source.looping));

        LogisticsSoundInstance instance = new LogisticsSoundInstance(
                source.baseLocation, source.pos, source.volume, source.pitch, source.radius, source.looping);
        source.instance = instance;
        Minecraft.getInstance().getSoundManager().play(instance);
    }

    private static void detach(ClientSource source) {
        ACTIVE_STREAMS.remove(source.pathLocation);
        if (source.instance != null) {
            Minecraft.getInstance().getSoundManager().stop(source.instance);
            source.instance = null;
        }
    }

    private static float clampPitch(float pitch) {
        return Math.max(0.5f, Math.min(2.0f, pitch));
    }
}

package com.restonic4.logistics.audio;

import com.restonic4.logistics.networking.ClientNetworking;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side on-disk cache of downloaded sounds, keyed by content hash so a file is fetched
 * from the server at most once and re-fetched automatically when its contents change. This is
 * what makes audio stations work on remote dedicated servers, where the client cannot read the
 * server's files directly.
 * <p>
 * All methods run on the client (render) thread.
 */
public class ClientAudioCache {
    private static File cacheDir;

    /** In-flight downloads keyed by hash, holding partial chunks + waiting callbacks. */
    private static final Map<String, Pending> PENDING = new HashMap<>();

    private static class Pending {
        final String soundId;
        final String ext;
        final byte[][] chunks;
        int received = 0;
        final List<Runnable> callbacks = new ArrayList<>();

        Pending(String soundId, String ext, int totalChunks) {
            this.soundId = soundId;
            this.ext = ext;
            this.chunks = new byte[totalChunks][];
        }
    }

    private static File dir() {
        if (cacheDir == null) {
            cacheDir = new File(Minecraft.getInstance().gameDirectory, "logistics/audio_cache");
            cacheDir.mkdirs();
        }
        return cacheDir;
    }

    private static String extOf(String soundId) {
        int dot = soundId.lastIndexOf('.');
        return dot >= 0 ? soundId.substring(dot) : ".wav";
    }

    private static File cacheFile(String hash, String ext) {
        return new File(dir(), hash + ext);
    }

    /** The cached file for this sound if already present, else null. */
    public static File getCached(String soundId, String hash) {
        if (hash == null || hash.isEmpty()) return null;
        File f = cacheFile(hash, extOf(soundId));
        return f.exists() ? f : null;
    }

    /**
     * Ensures the sound is available locally, then runs {@code onReady} with the cache file.
     * If cached, runs immediately; otherwise requests it from the server and defers until the
     * download completes. Callbacks for the same file are coalesced into one request.
     */
    public static void ensure(String soundId, String hash, java.util.function.Consumer<File> onReady) {
        if (hash == null || hash.isEmpty()) return;

        File cached = getCached(soundId, hash);
        if (cached != null) {
            onReady.accept(cached);
            return;
        }

        Pending pending = PENDING.get(hash);
        boolean firstRequest = pending == null;
        if (firstRequest) {
            // totalChunks unknown until first chunk arrives; start a placeholder.
            pending = new Pending(soundId, extOf(soundId), 0);
            PENDING.put(hash, pending);
        }
        pending.callbacks.add(() -> {
            File f = getCached(soundId, hash);
            if (f != null) onReady.accept(f);
        });

        if (firstRequest) {
            ClientNetworking.sendToServer(new AudioRequestC2SPacket(soundId));
        }
    }

    /** Receives one chunk of an in-flight download; writes the file and fires callbacks when complete. */
    public static void receiveChunk(String soundId, String hash, int chunkIndex, int totalChunks, byte[] chunk) {
        if (hash == null || hash.isEmpty()) return;

        Pending pending = PENDING.get(hash);
        if (pending == null || pending.chunks.length != totalChunks) {
            // Either no request is tracked or this is the first chunk defining the size; (re)allocate.
            Pending fresh = new Pending(soundId, extOf(soundId), totalChunks);
            if (pending != null) fresh.callbacks.addAll(pending.callbacks);
            pending = fresh;
            PENDING.put(hash, pending);
        }

        if (chunkIndex < 0 || chunkIndex >= pending.chunks.length) return;
        if (pending.chunks[chunkIndex] == null) {
            pending.chunks[chunkIndex] = chunk;
            pending.received++;
        }

        if (pending.received < pending.chunks.length) return;

        // Assemble and write atomically (temp file then rename) so a half-written cache entry
        // is never picked up as valid.
        int total = 0;
        for (byte[] c : pending.chunks) total += c.length;
        byte[] full = new byte[total];
        int p = 0;
        for (byte[] c : pending.chunks) {
            System.arraycopy(c, 0, full, p, c.length);
            p += c.length;
        }

        File target = cacheFile(hash, pending.ext);
        try {
            File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
            java.nio.file.Files.write(tmp.toPath(), full);
            java.nio.file.Files.move(tmp.toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("Failed to write audio cache for " + soundId + ": " + e.getMessage());
            PENDING.remove(hash);
            return;
        }

        List<Runnable> callbacks = pending.callbacks;
        PENDING.remove(hash);
        for (Runnable cb : callbacks) cb.run();
    }
}

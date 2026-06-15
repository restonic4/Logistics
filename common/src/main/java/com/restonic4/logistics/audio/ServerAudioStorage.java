package com.restonic4.logistics.audio;

import com.restonic4.logistics.Constants;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerAudioStorage {
    private static File baseDir;

    private record HashEntry(long lastModified, long length, String hash) {}
    private static final Map<String, HashEntry> HASH_CACHE = new ConcurrentHashMap<>();

    public static void init(File serverRoot) {
        baseDir = new File(serverRoot, "logistics/sounds");
        baseDir.mkdirs();

        Constants.LOG.warn("Initializing server audio storage at {}", baseDir);
    }

    public static File getPlayerFolder(UUID playerId) {
        return new File(baseDir, playerId.toString());
    }

    public static File getSoundFile(String soundId) {
        if (soundId == null || soundId.isEmpty()) return null;
        if (soundId.contains("..")) return null;
        File file = new File(baseDir, soundId);
        try {
            if (!file.getCanonicalPath().startsWith(baseDir.getCanonicalPath())) return null;
        } catch (Exception e) { return null; }
        return file;
    }

    public static List<String> getAllSounds() {
        List<String> sounds = new ArrayList<>();
        if (baseDir == null || !baseDir.exists()) return sounds;
        File[] folders = baseDir.listFiles(File::isDirectory);
        if (folders == null) return sounds;
        for (File folder : folders) {
            String uuid = folder.getName();
            File[] files = folder.listFiles((dir, name) -> {
                String n = name.toLowerCase();
                return n.endsWith(".wav") || n.endsWith(".ogg");
            });
            if (files == null) continue;
            for (File f : files) {
                sounds.add(uuid + "/" + f.getName());
            }
        }
        return sounds;
    }

    public static boolean deleteSound(String soundId) {
        File f = getSoundFile(soundId);
        boolean deleted = f != null && f.exists() && f.delete();
        if (deleted) HASH_CACHE.remove(soundId);
        return deleted;
    }

    /**
     * Content hash (hex) of a stored sound, used as the client cache key so clients only
     * download a file once and re-fetch automatically when its contents change. Cached and
     * keyed by (lastModified, length) so it is cheap to call repeatedly. Returns null when
     * the file is missing.
     */
    public static String getHash(String soundId) {
        File f = getSoundFile(soundId);
        if (f == null || !f.exists()) return null;

        HashEntry cached = HASH_CACHE.get(soundId);
        if (cached != null && cached.lastModified == f.lastModified() && cached.length == f.length()) {
            return cached.hash;
        }

        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            String hash = sb.toString();
            HASH_CACHE.put(soundId, new HashEntry(f.lastModified(), f.length(), hash));
            return hash;
        } catch (Exception e) {
            Constants.LOG.warn("Failed to hash sound {}: {}", soundId, e.getMessage());
            return null;
        }
    }

    /** Raw file bytes for a stored sound, or null if missing/invalid. Used to serve downloads. */
    public static byte[] readBytes(String soundId) {
        File f = getSoundFile(soundId);
        if (f == null || !f.exists()) return null;
        try {
            return java.nio.file.Files.readAllBytes(f.toPath());
        } catch (Exception e) {
            Constants.LOG.warn("Failed to read sound {}: {}", soundId, e.getMessage());
            return null;
        }
    }

    public static File getBaseDir() {
        return baseDir;
    }

    public static void clear() {
        baseDir = null;
        Constants.LOG.warn("Server audio storage cleaned");
    }
}
package com.restonic4.logistics.audio;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServerAudioStorage {
    private static File baseDir;

    public static void init(File serverRoot) {
        baseDir = new File(serverRoot, "logistics/sounds");
        baseDir.mkdirs();
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
            File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));
            if (files == null) continue;
            for (File f : files) {
                sounds.add(uuid + "/" + f.getName());
            }
        }
        return sounds;
    }

    public static boolean deleteSound(String soundId) {
        File f = getSoundFile(soundId);
        return f != null && f.exists() && f.delete();
    }
}
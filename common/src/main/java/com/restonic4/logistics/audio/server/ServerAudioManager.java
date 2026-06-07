package com.restonic4.logistics.audio.server;

import com.restonic4.logistics.audio.AudioFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerAudioManager {
    private static final ServerAudioManager INSTANCE = new ServerAudioManager();
    private final Map<UUID, ServerAudioSource> sources = new ConcurrentHashMap<>();

    public static ServerAudioManager getInstance() {
        return INSTANCE;
    }

    public UUID playSound(ServerLevel level, Vec3 position, ResourceLocation sound, AudioFormat format,
                          float volume, float pitch, float radius) {
        UUID id = UUID.randomUUID();
        ServerAudioSource source = new ServerAudioSource(id, level, position, sound, null, format, false, volume, pitch, radius);
        sources.put(id, source);
        source.play();
        return id;
    }

    public UUID playRadio(ServerLevel level, Vec3 position, String url, AudioFormat format,
                          float volume, float pitch, float radius) {
        UUID id = UUID.randomUUID();
        ServerAudioSource source = new ServerAudioSource(id, level, position, null, url, format, true, volume, pitch, radius);
        sources.put(id, source);
        source.play();
        return id;
    }

    public void stop(UUID id) {
        ServerAudioSource source = sources.remove(id);
        if (source != null) source.stop();
    }

    public void setVolume(UUID id, float volume) {
        ServerAudioSource source = sources.get(id);
        if (source != null) source.setVolume(volume);
    }

    public void setPitch(UUID id, float pitch) {
        ServerAudioSource source = sources.get(id);
        if (source != null) source.setPitch(pitch);
    }

    public void setRadius(UUID id, float radius) {
        ServerAudioSource source = sources.get(id);
        if (source != null) source.setRadius(radius);
    }

    public void setPosition(UUID id, Vec3 position) {
        ServerAudioSource source = sources.get(id);
        if (source != null) source.setPosition(position);
    }

    public void tick() {
        sources.values().forEach(ServerAudioSource::tick);
        sources.values().removeIf(ServerAudioSource::isStopped);
    }

    public void stopAll() {
        sources.values().forEach(ServerAudioSource::stop);
        sources.clear();
    }
}
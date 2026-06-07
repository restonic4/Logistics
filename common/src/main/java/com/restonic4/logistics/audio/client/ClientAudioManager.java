package com.restonic4.logistics.audio.client;

import com.restonic4.logistics.audio.network.ClientboundAudioPlayPacket;
import com.restonic4.logistics.audio.network.ClientboundAudioStopPacket;
import com.restonic4.logistics.audio.network.ClientboundAudioSyncPacket;
import com.restonic4.logistics.audio.network.ClientboundAudioUpdatePacket;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientAudioManager {
    private static final ClientAudioManager INSTANCE = new ClientAudioManager();
    private final Map<UUID, ClientAudioSource> sources = new ConcurrentHashMap<>();

    public static ClientAudioManager getInstance() {
        return INSTANCE;
    }

    public void handlePlay(ClientboundAudioPlayPacket packet) {
        Thread t = new Thread(() -> {
            try {
                ClientAudioSource source = new ClientAudioSource(packet);
                Minecraft.getInstance().execute(() -> {
                    sources.put(packet.getSourceId(), source);
                    source.play();
                });
            } catch (Exception e) {
                System.err.println("Failed to start audio source " + packet.getSourceId() + ": " + e.getMessage());
            }
        }, "AudioLoad-" + packet.getSourceId());
        t.setDaemon(true);
        t.start();
    }

    public void handleStop(ClientboundAudioStopPacket packet) {
        Minecraft.getInstance().execute(() -> {
            ClientAudioSource source = sources.remove(packet.getSourceId());
            if (source != null) source.destroy();
        });
    }

    public void handleUpdate(ClientboundAudioUpdatePacket packet) {
        Minecraft.getInstance().execute(() -> {
            ClientAudioSource source = sources.get(packet.getSourceId());
            if (source != null) source.update(packet);
        });
    }

    public void handleSync(ClientboundAudioSyncPacket packet) {
        Minecraft.getInstance().execute(() -> {
            ClientAudioSource source = sources.get(packet.getSourceId());
            if (source != null) source.sync(packet);
        });
    }

    public void tick() {
        sources.values().forEach(ClientAudioSource::tick);
        sources.values().removeIf(ClientAudioSource::isFinished);
    }

    public void stopAll() {
        sources.values().forEach(ClientAudioSource::destroy);
        sources.clear();
    }
}
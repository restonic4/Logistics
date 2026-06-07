package com.restonic4.logistics.audio.client;

import com.restonic4.logistics.audio.AudioDecoders;
import com.restonic4.logistics.audio.AudioFormat;
import com.restonic4.logistics.audio.PcmAudioStream;
import com.restonic4.logistics.audio.network.ClientboundAudioPlayPacket;
import com.restonic4.logistics.audio.network.ClientboundAudioSyncPacket;
import com.restonic4.logistics.audio.network.ClientboundAudioUpdatePacket;
import com.restonic4.logistics.audio.stream.SharedBufferAudioStream;
import com.restonic4.logistics.audio.stream.SharedNetworkStream;
import com.restonic4.logistics.audio.stream.SharedNetworkStreamManager;
import com.restonic4.logistics.audio.stream.StreamingAudioSource;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class ClientAudioSource {
    private final UUID id;
    private Vec3 position;
    private float volume;
    private float pitch;
    private float radius;
    private final boolean isRadio;
    private final String url;
    private final ResourceLocation soundId;
    private final AudioFormat format;
    private final PcmAudioStream stream;
    private final StreamingAudioSource alSource;
    private boolean playing = false;

    public ClientAudioSource(ClientboundAudioPlayPacket packet) throws Exception {
        this.id = packet.getSourceId();
        this.position = packet.getPosition();
        this.volume = packet.getVolume();
        this.pitch = packet.getPitch();
        this.radius = packet.getRadius();
        this.isRadio = packet.isRadio();
        this.url = packet.getUrl();
        this.soundId = packet.getSoundId();
        this.format = packet.getFormat();

        if (isRadio) {
            SharedNetworkStream shared = SharedNetworkStreamManager.getInstance().acquire(url, format);
            this.stream = new SharedBufferAudioStream(shared);
        } else {
            this.stream = loadFileStream(soundId, format, packet.getElapsedMs());
        }

        this.alSource = new StreamingAudioSource(stream);
        updateAlProperties();
    }

    private PcmAudioStream loadFileStream(ResourceLocation id, AudioFormat format, long elapsedMs) throws Exception {
        if (id == null) throw new IOException("No sound ID provided");
        Resource resource = Minecraft.getInstance().getResourceManager().getResource(id).orElseThrow();
        try (InputStream in = resource.open()) {
            PcmAudioStream pcm = AudioDecoders.getDecoder(format).decodeFull(in);
            if (elapsedMs > 0 && pcm.isSeekable()) pcm.seek(elapsedMs);
            return pcm;
        }
    }

    public void tick() {
        if (!playing) return;
        alSource.updatePosition(position);
        alSource.tick();
    }

    public void update(ClientboundAudioUpdatePacket packet) {
        this.position = packet.getPosition();
        this.volume = packet.getVolume();
        this.pitch = packet.getPitch();
        this.radius = packet.getRadius();
        updateAlProperties();
    }

    public void sync(ClientboundAudioSyncPacket packet) {
        if (isRadio || !stream.isSeekable()) return;
        long diff = Math.abs(packet.getElapsedMs() - getLocalElapsed());
        if (diff > 2000) {
            alSource.seek(packet.getElapsedMs());
        }
    }

    private long getLocalElapsed() {
        return 0;
    }

    public void play() {
        playing = true;
        alSource.play();
    }

    public void stop() {
        playing = false;
        alSource.stop();
    }

    public void destroy() {
        stop();
        alSource.destroy();
        if (isRadio && url != null) {
            SharedNetworkStreamManager.getInstance().release(url);
        }
    }

    public boolean isFinished() {
        return alSource.isFinished();
    }

    private void updateAlProperties() {
        alSource.updatePosition(position);
        alSource.updateVolume(volume);
        alSource.updatePitch(pitch);
        alSource.updateRadius(radius);
    }

    public UUID getId() { return id; }
}
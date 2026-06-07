package com.restonic4.logistics.audio.network;

import com.restonic4.logistics.audio.client.ClientAudioManager;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class ClientboundAudioSyncPacket implements S2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio_sync");

    private final UUID sourceId;
    private final long elapsedMs;
    private final boolean playing;

    public ClientboundAudioSyncPacket(UUID sourceId, long elapsedMs, boolean playing) {
        this.sourceId = sourceId;
        this.elapsedMs = elapsedMs;
        this.playing = playing;
    }

    public ClientboundAudioSyncPacket(FriendlyByteBuf buf) {
        this.sourceId = buf.readUUID();
        this.elapsedMs = buf.readLong();
        this.playing = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(sourceId);
        buf.writeLong(elapsedMs);
        buf.writeBoolean(playing);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> ClientAudioManager.getInstance().handleSync(this));
    }

    public UUID getSourceId() { return sourceId; }
    public long getElapsedMs() { return elapsedMs; }
    public boolean isPlaying() { return playing; }
}
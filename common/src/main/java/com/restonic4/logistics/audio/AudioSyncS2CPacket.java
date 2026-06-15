package com.restonic4.logistics.audio;

import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Periodic authoritative playback offset for a source. The client compares it to its own
 * estimated offset and, if drift is audible, cleanly restarts playback at the corrected
 * offset. Because every client resyncs to the same server clock, separate stations stay in
 * phase and re-entering a radius never accumulates drift.
 */
public class AudioSyncS2CPacket implements S2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio/sync");

    private final UUID sourceId;
    private final long elapsedMs;

    public AudioSyncS2CPacket(UUID sourceId, long elapsedMs) {
        this.sourceId = sourceId;
        this.elapsedMs = elapsedMs;
    }

    public AudioSyncS2CPacket(FriendlyByteBuf buf) {
        this.sourceId = buf.readUUID();
        this.elapsedMs = buf.readLong();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(sourceId);
        buf.writeLong(elapsedMs);
    }

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> ClientAudioManager.resync(sourceId, elapsedMs));
    }
}

package com.restonic4.logistics.audio;

import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → client chunk of a requested sound file. Chunks are reassembled and written to the
 * client cache by {@link ClientAudioCache}; once complete, any playback waiting on the file
 * starts.
 */
public class AudioDownloadS2CPacket implements S2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio/download");

    private final String soundId;
    private final String hash;
    private final int chunkIndex;
    private final int totalChunks;
    private final byte[] chunk;

    public AudioDownloadS2CPacket(String soundId, String hash, int chunkIndex, int totalChunks, byte[] chunk) {
        this.soundId = soundId;
        this.hash = hash;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.chunk = chunk;
    }

    public AudioDownloadS2CPacket(FriendlyByteBuf buf) {
        this.soundId = buf.readUtf();
        this.hash = buf.readUtf();
        this.chunkIndex = buf.readInt();
        this.totalChunks = buf.readInt();
        this.chunk = buf.readByteArray();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(soundId);
        buf.writeUtf(hash);
        buf.writeInt(chunkIndex);
        buf.writeInt(totalChunks);
        buf.writeByteArray(chunk);
    }

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> ClientAudioCache.receiveChunk(soundId, hash, chunkIndex, totalChunks, chunk));
    }
}

package com.restonic4.logistics.audio;

import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networking.ServerNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → server request for the bytes of a stored sound the client does not have cached.
 * The server replies by streaming the file back in {@link AudioDownloadS2CPacket} chunks.
 */
public class AudioRequestC2SPacket implements C2SPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio/request");

    private static final int CHUNK_SIZE = 30000; // under the 32767 custom-payload limit with header overhead

    private final String soundId;

    public AudioRequestC2SPacket(String soundId) {
        this.soundId = soundId;
    }

    public AudioRequestC2SPacket(FriendlyByteBuf buf) {
        this.soundId = buf.readUtf();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(soundId);
    }

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        byte[] bytes = ServerAudioStorage.readBytes(soundId);
        if (bytes == null) return;
        String hash = ServerAudioStorage.getHash(soundId);
        if (hash == null) return;

        int totalChunks = Math.max(1, (bytes.length + CHUNK_SIZE - 1) / CHUNK_SIZE);
        for (int i = 0; i < totalChunks; i++) {
            int from = i * CHUNK_SIZE;
            int to = Math.min(from + CHUNK_SIZE, bytes.length);
            byte[] chunk = new byte[to - from];
            System.arraycopy(bytes, from, chunk, 0, chunk.length);
            ServerNetworking.sendToClient(player,
                    new AudioDownloadS2CPacket(soundId, hash, i, totalChunks, chunk));
        }
    }
}

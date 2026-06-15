package com.restonic4.logistics.blocks.audio_station;

import com.restonic4.logistics.audio.AudioUtils;
import com.restonic4.logistics.audio.ServerAudioStorage;
import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networking.ServerNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AudioUploadPacket implements C2SPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio_station/upload");

    private static final Map<UUID, UploadSession> sessions = new HashMap<>();

    private final String filename;
    private final byte[] chunk;
    private final int chunkIndex;
    private final int totalChunks;

    public AudioUploadPacket(String filename, byte[] chunk, int chunkIndex, int totalChunks) {
        this.filename = filename;
        this.chunk = chunk;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
    }

    public AudioUploadPacket(FriendlyByteBuf buf) {
        this.filename = buf.readUtf();
        this.chunk = buf.readByteArray();
        this.chunkIndex = buf.readInt();
        this.totalChunks = buf.readInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(filename);
        buf.writeByteArray(chunk);
        buf.writeInt(chunkIndex);
        buf.writeInt(totalChunks);
    }

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        String lowerName = filename.toLowerCase();
        if (!lowerName.endsWith(".wav") && !lowerName.endsWith(".ogg")) {
            System.err.println("Rejected unsupported audio upload (only .wav/.ogg) from " + player.getUUID());
            sessions.remove(player.getUUID());
            return;
        }

        UUID playerId = player.getUUID();
        UploadSession session = sessions.computeIfAbsent(playerId, k -> new UploadSession(totalChunks));
        session.addChunk(chunkIndex, chunk);

        if (session.isComplete()) {
            byte[] full = session.assemble();
            sessions.remove(playerId);

            if (full.length > 15 * 1024 * 1024) { // 15MB max
                System.err.println("Rejected oversized upload from " + playerId);
                return;
            }

            File folder = ServerAudioStorage.getPlayerFolder(playerId);
            folder.mkdirs();
            File target = new File(folder, filename);

            try {
                java.nio.file.Files.write(target.toPath(), full);
                // Validate by parsing headers (works for both wav and ogg, server-side, no decoder).
                AudioUtils.getAudioDurationMs(target.getAbsolutePath());
                System.out.println("Accepted sound upload: " + target.getAbsolutePath());
                ServerNetworking.sendToAll(server, new UploadedAudiosSyncPacket(ServerAudioStorage.getAllSounds()));
            } catch (Exception e) {
                System.err.println("Invalid WAV from " + playerId + ": " + e.getMessage());
                target.delete();
            }
        }
    }

    private static class UploadSession {
        private final byte[][] chunks;
        private int received = 0;

        UploadSession(int total) {
            this.chunks = new byte[total][];
        }

        void addChunk(int index, byte[] data) {
            if (index >= 0 && index < chunks.length && chunks[index] == null) {
                chunks[index] = data;
                received++;
            }
        }

        boolean isComplete() {
            return received == chunks.length;
        }

        byte[] assemble() {
            int total = 0;
            for (byte[] c : chunks) total += c.length;
            byte[] result = new byte[total];
            int pos = 0;
            for (byte[] c : chunks) {
                System.arraycopy(c, 0, result, pos, c.length);
                pos += c.length;
            }
            return result;
        }
    }
}
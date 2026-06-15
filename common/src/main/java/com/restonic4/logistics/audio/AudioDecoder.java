package com.restonic4.logistics.audio;

import com.mojang.blaze3d.audio.OggAudioStream;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decodes a cached WAV/OGG file into raw PCM once (cached by file path) and serves it through
 * a {@link AudioStream} that supports starting at an arbitrary offset and seamless looping.
 * The decoded PCM is fed to Minecraft's sound engine, which owns the OpenAL channel lifecycle.
 */
public class AudioDecoder {
    private record PcmData(byte[] data, AudioFormat format, int frameSize) {}

    private static final Map<String, PcmData> DECODE_CACHE = new ConcurrentHashMap<>();

    /**
     * Opens a playback stream for the given decoded file, starting {@code startMs} into the
     * audio and optionally looping. Throws if the file cannot be decoded.
     */
    public static AudioStream open(File file, long startMs, boolean loop) throws Exception {
        PcmData pcm = decode(file);
        return new PcmAudioStream(pcm, startMs, loop);
    }

    /** Decoded duration in ms (0 if it can't be decoded). Used for drift comparison. */
    public static long getDurationMs(File file) {
        try {
            PcmData pcm = decode(file);
            long bytesPerSecond = (long) pcm.format.getSampleRate() * pcm.frameSize;
            if (bytesPerSecond <= 0) return 0;
            return (pcm.data.length * 1000L) / bytesPerSecond;
        } catch (Exception e) {
            return 0;
        }
    }

    private static PcmData decode(File file) throws Exception {
        String key = file.getAbsolutePath();
        PcmData cached = DECODE_CACHE.get(key);
        if (cached != null) return cached;

        String lower = key.toLowerCase();
        byte[] data;
        AudioFormat format;

        if (lower.endsWith(".ogg")) {
            try (FileInputStream in = new FileInputStream(file);
                 OggAudioStream ogg = new OggAudioStream(in)) {
                ByteBuffer buf = ogg.readAll();
                data = new byte[buf.remaining()];
                buf.get(data);
                format = ogg.getFormat();
            }
        } else {
            AudioUtils.LoadedAudioData loaded = AudioUtils.loadWav(key);
            data = new byte[loaded.data.remaining()];
            loaded.data.get(data);
            format = loaded.format;
        }

        int frameSize = format.getChannels() * (format.getSampleSizeInBits() / 8);
        if (frameSize <= 0) frameSize = 1;
        PcmData result = new PcmData(data, format, frameSize);
        DECODE_CACHE.put(key, result);
        return result;
    }

    /** In-memory PCM stream with offset start and optional looping; returns direct buffers for LWJGL. */
    private static class PcmAudioStream implements AudioStream {
        private final PcmData pcm;
        private final boolean loop;
        private int position;

        PcmAudioStream(PcmData pcm, long startMs, boolean loop) {
            this.pcm = pcm;
            this.loop = loop;

            long bytesPerSecond = (long) pcm.format.getSampleRate() * pcm.frameSize;
            long startByte = (startMs * bytesPerSecond) / 1000L;
            startByte -= startByte % pcm.frameSize; // frame-align
            int total = pcm.data.length;
            if (total > 0) {
                if (loop) {
                    startByte %= total;
                    startByte -= startByte % pcm.frameSize;
                } else if (startByte > total) {
                    startByte = total;
                }
            }
            this.position = (int) Math.max(0, startByte);
        }

        @Override
        public AudioFormat getFormat() {
            return pcm.format;
        }

        @Override
        public ByteBuffer read(int requested) {
            int want = (requested / pcm.frameSize) * pcm.frameSize;
            if (want <= 0) want = pcm.frameSize;

            ByteBuffer out = BufferUtils.createByteBuffer(want);
            int total = pcm.data.length;
            int written = 0;
            while (written < want) {
                if (position >= total) {
                    if (loop && total > 0) {
                        position = 0;
                    } else {
                        break;
                    }
                }
                int n = Math.min(total - position, want - written);
                out.put(pcm.data, position, n);
                position += n;
                written += n;
            }
            out.flip();
            return out;
        }

        @Override
        public void close() {
            // Nothing to release: PCM is owned by the shared decode cache.
        }
    }
}

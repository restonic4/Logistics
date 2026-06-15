package com.restonic4.logistics.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioUtils {
    public static class LoadedAudioData {
        public final ByteBuffer data;
        public final AudioFormat format;
        public final boolean mono;

        public LoadedAudioData(ByteBuffer data, AudioFormat format, boolean mono) {
            this.data = data;
            this.format = format;
            this.mono = mono;
        }
    }

    public static LoadedAudioData loadWav(String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) throw new RuntimeException("Audio file not found: " + path);

        try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
            AudioFormat format = stream.getFormat();

            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED
                    && format.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {
                throw new RuntimeException("Unsupported WAV encoding: " + format.getEncoding() + ". Only PCM is supported.");
            }

            // Stereo is allowed: we compute distance gain ourselves (see LogisticsSoundInstance),
            // so volume/radius still work. Only OpenAL's directional panning is mono-only.
            boolean mono = format.getChannels() == 1;

            byte[] bytes = stream.readAllBytes();
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
            buffer.put(bytes).flip();
            return new LoadedAudioData(buffer, format, mono);
        }
    }

    public static int getOpenAlFormat(AudioFormat format) {
        int channels = format.getChannels();
        int sampleSize = format.getSampleSizeInBits();
        if (channels == 1) {
            return sampleSize == 8 ? org.lwjgl.openal.AL10.AL_FORMAT_MONO8 : org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
        } else {
            return sampleSize == 8 ? org.lwjgl.openal.AL10.AL_FORMAT_STEREO8 : org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
        }
    }

    /** Returns the playback duration in milliseconds, dispatching by file extension. */
    public static long getAudioDurationMs(String path) throws Exception {
        String lower = path.toLowerCase();
        if (lower.endsWith(".ogg")) return getOggDurationMs(path);
        return getWavDurationMs(path);
    }

    /**
     * Reads the duration of an Ogg/Vorbis file from its headers only, without decoding —
     * the decoder (STB/JOrbis) is client-only and absent on a dedicated server. Duration is
     * the last page's granule position (total PCM samples) divided by the Vorbis sample rate.
     */
    public static long getOggDurationMs(String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) throw new RuntimeException("Audio file not found: " + path);

        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            long len = raf.length();

            // Sample rate lives in the Vorbis identification header, which is the first packet
            // of the first ("OggS") page: 0x01 "vorbis" version(4) channels(1) sampleRate(4 LE).
            int sampleRate = readOggSampleRate(raf);
            if (sampleRate <= 0) throw new RuntimeException("Could not read Ogg sample rate");

            // Total samples = granule position of the final page. Scan backwards for the last
            // "OggS" capture pattern and read its 64-bit little-endian granule position.
            int scan = (int) Math.min(len, 65536L);
            byte[] tail = new byte[scan];
            raf.seek(len - scan);
            raf.readFully(tail);

            long granule = -1;
            for (int i = scan - 27; i >= 0; i--) {
                if (tail[i] == 'O' && tail[i + 1] == 'g' && tail[i + 2] == 'g' && tail[i + 3] == 'S') {
                    long g = 0;
                    for (int b = 0; b < 8; b++) {
                        g |= (tail[i + 6 + b] & 0xFFL) << (8 * b);
                    }
                    if (g != -1L) { granule = g; break; }
                }
            }
            if (granule < 0) throw new RuntimeException("Could not read Ogg granule position");

            return (granule * 1000L) / sampleRate;
        }
    }

    private static int readOggSampleRate(RandomAccessFile raf) throws Exception {
        int head = (int) Math.min(raf.length(), 65536L);
        byte[] buf = new byte[head];
        raf.seek(0);
        raf.readFully(buf);

        // Find the "vorbis" identification packet marker (0x01 'vorbis').
        for (int i = 0; i + 16 < buf.length; i++) {
            if (buf[i] == 0x01 && buf[i + 1] == 'v' && buf[i + 2] == 'o' && buf[i + 3] == 'r'
                    && buf[i + 4] == 'b' && buf[i + 5] == 'i' && buf[i + 6] == 's') {
                int p = i + 7 + 4 + 1; // skip "vorbis", vorbis_version(4), audio_channels(1)
                return (buf[p] & 0xFF) | ((buf[p + 1] & 0xFF) << 8)
                        | ((buf[p + 2] & 0xFF) << 16) | ((buf[p + 3] & 0xFF) << 24);
            }
        }
        return -1;
    }

    public static long getWavDurationMs(String path) throws Exception {
        try (RandomAccessFile file = new RandomAccessFile(path, "r")) {
            byte[] riff = new byte[4];
            file.readFully(riff);
            if (!new String(riff, "ASCII").equals("RIFF")) throw new RuntimeException("Not a RIFF file");

            file.skipBytes(4);
            byte[] wave = new byte[4];
            file.readFully(wave);
            if (!new String(wave, "ASCII").equals("WAVE")) throw new RuntimeException("Not a WAVE file");

            int sampleRate = 0, channels = 0, bitsPerSample = 0;
            long dataSize = 0;

            while (file.getFilePointer() < file.length()) {
                byte[] chunkId = new byte[4];
                file.readFully(chunkId);
                int chunkSize = Integer.reverseBytes(file.readInt());
                String id = new String(chunkId, "ASCII");

                if (id.equals("fmt ")) {
                    file.skipBytes(2);
                    channels = Short.reverseBytes(file.readShort());
                    sampleRate = Integer.reverseBytes(file.readInt());
                    file.skipBytes(6);
                    bitsPerSample = Short.reverseBytes(file.readShort());
                    if (chunkSize > 16) file.skipBytes(chunkSize - 16);
                } else if (id.equals("data")) {
                    dataSize = chunkSize & 0xFFFFFFFFL;
                    break;
                } else {
                    file.skipBytes(chunkSize);
                }
            }

            if (sampleRate == 0 || channels == 0 || bitsPerSample == 0 || dataSize == 0) {
                throw new RuntimeException("Invalid WAV header");
            }

            long bytesPerSecond = sampleRate * channels * bitsPerSample / 8;
            return (dataSize * 1000L) / bytesPerSecond;
        }
    }
}
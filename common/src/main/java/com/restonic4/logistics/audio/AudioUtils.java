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

            boolean mono = format.getChannels() == 1;
            if (!mono) {
                throw new RuntimeException("This audio file has more than 1 channel, it needs to be mono!");
            }

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
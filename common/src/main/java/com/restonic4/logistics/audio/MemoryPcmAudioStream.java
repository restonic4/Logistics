package com.restonic4.logistics.audio;

import java.io.IOException;

public class MemoryPcmAudioStream implements PcmAudioStream {
    private final byte[] data;
    private final int sampleRate;
    private final int channels;
    private int position;

    public MemoryPcmAudioStream(byte[] data, int sampleRate, int channels) {
        this.data = data;
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    @Override
    public int read(byte[] buffer) {
        int available = data.length - position;
        int toRead = Math.min(buffer.length, available);
        if (toRead <= 0) return -1;
        System.arraycopy(data, position, buffer, 0, toRead);
        position += toRead;
        return toRead;
    }

    @Override
    public void seek(long milliseconds) {
        int bytePos = (int) (milliseconds * sampleRate * channels * 2L / 1000L);
        this.position = Math.max(0, Math.min(bytePos, data.length));
    }

    @Override
    public void close() {
        this.position = data.length;
    }

    @Override
    public int getSampleRate() { return sampleRate; }

    @Override
    public int getChannels() { return channels; }

    @Override
    public boolean isSeekable() { return true; }
}
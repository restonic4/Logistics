package com.restonic4.logistics.audio.stream;

import com.restonic4.logistics.audio.PcmAudioStream;

import java.io.IOException;
import java.util.UUID;

public class SharedBufferAudioStream implements PcmAudioStream {
    private final SharedNetworkStream stream;
    private final UUID readerId = UUID.randomUUID();

    public SharedBufferAudioStream(SharedNetworkStream stream) {
        this.stream = stream;
        stream.getRingBuffer().registerReader(readerId);
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        int read = stream.getRingBuffer().read(readerId, buffer);
        if (read == 0 && !stream.isRunning()) {
            return -1;
        }
        return read;
    }

    @Override
    public void seek(long milliseconds) {}

    @Override
    public void close() throws IOException {
        stream.getRingBuffer().unregisterReader(readerId);
    }

    @Override
    public int getSampleRate() { return stream.getSampleRate(); }

    @Override
    public int getChannels() { return stream.getChannels(); }

    @Override
    public boolean isSeekable() { return false; }
}
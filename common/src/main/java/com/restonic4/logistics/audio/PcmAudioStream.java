package com.restonic4.logistics.audio;

import java.io.IOException;

public interface PcmAudioStream {
    int read(byte[] buffer) throws IOException;
    void seek(long milliseconds) throws IOException;
    void close() throws IOException;
    int getSampleRate();
    int getChannels();
    boolean isSeekable();
}
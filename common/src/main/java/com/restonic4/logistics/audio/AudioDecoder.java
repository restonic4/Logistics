package com.restonic4.logistics.audio;

import java.io.IOException;
import java.io.InputStream;

public interface AudioDecoder {
    PcmAudioStream decodeFull(InputStream input) throws IOException;
    void decodeStreaming(InputStream input, java.io.OutputStream output) throws IOException;
}
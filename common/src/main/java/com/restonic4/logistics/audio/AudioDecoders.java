package com.restonic4.logistics.audio;

import com.restonic4.logistics.audio.codec.Mp3Decoder;
import com.restonic4.logistics.audio.codec.OggDecoder;
import com.restonic4.logistics.audio.codec.WavDecoder;

public class AudioDecoders {
    public static AudioDecoder getDecoder(AudioFormat format) {
        return switch (format) {
            case OGG -> new OggDecoder();
            case WAV -> new WavDecoder();
            case MP3 -> new Mp3Decoder();
        };
    }
}
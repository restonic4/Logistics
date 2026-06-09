package com.restonic4.logistics.audio;

import org.lwjgl.openal.AL10;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

public class AudioBuffer {
    private final int alBufferId;
    private final AudioFormat format;
    private final boolean mono;

    private AudioBuffer(int alBufferId, AudioFormat format, boolean mono) {
        this.alBufferId = alBufferId;
        this.format = format;
        this.mono = mono;
    }

    public static AudioBuffer load(String path) throws Exception {
        AudioUtils.LoadedAudioData loaded = AudioUtils.loadWav(path);
        int alBuffer = AL10.alGenBuffers();
        int alFormat = AudioUtils.getOpenAlFormat(loaded.format);
        AL10.alBufferData(alBuffer, alFormat, loaded.data, (int) loaded.format.getSampleRate());
        return new AudioBuffer(alBuffer, loaded.format, loaded.mono);
    }

    public int getAlBufferId() {
        return alBufferId;
    }

    public boolean isMono() {
        return mono;
    }

    public void discard() {
        AL10.alDeleteBuffers(alBufferId);
    }
}
package com.restonic4.logistics.audio.codec;

import com.restonic4.logistics.audio.AudioDecoder;
import com.restonic4.logistics.audio.MemoryPcmAudioStream;
import com.restonic4.logistics.audio.PcmAudioStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WavDecoder implements AudioDecoder {
    @Override
    public PcmAudioStream decodeFull(InputStream input) throws IOException {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(input);
            AudioFormat base = ais.getFormat();
            AudioFormat target = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    base.getSampleRate(), 16, base.getChannels(),
                    base.getChannels() * 2, base.getSampleRate(), false
            );
            AudioInputStream converted = AudioSystem.getAudioInputStream(target, ais);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = converted.read(buf)) > 0) baos.write(buf, 0, r);
            converted.close();
            return new MemoryPcmAudioStream(baos.toByteArray(), (int) target.getSampleRate(), target.getChannels());
        } catch (UnsupportedAudioFileException e) {
            throw new IOException("WAV decode failed", e);
        }
    }

    @Override
    public void decodeStreaming(InputStream input, OutputStream output) throws IOException {
        PcmAudioStream full = decodeFull(input);
        byte[] buf = new byte[4096];
        int r;
        while ((r = full.read(buf)) > 0) output.write(buf, 0, r);
        full.close();
    }
}
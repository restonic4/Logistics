package com.restonic4.logistics.audio.codec;

import com.restonic4.logistics.audio.AudioDecoder;
import com.restonic4.logistics.audio.MemoryPcmAudioStream;
import com.restonic4.logistics.audio.PcmAudioStream;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Mp3Decoder implements AudioDecoder {
    @Override
    public PcmAudioStream decodeFull(InputStream input) throws IOException {
        try {
            Bitstream bitstream = new Bitstream(input);
            Decoder decoder = new Decoder();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int sampleRate = -1;
            int channels = -1;

            while (true) {
                Header h = bitstream.readFrame();
                if (h == null) break;
                if (sampleRate == -1) {
                    sampleRate = h.frequency();
                    channels = h.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
                }
                SampleBuffer sb = (SampleBuffer) decoder.decodeFrame(h, bitstream);
                short[] pcm = sb.getBuffer();
                int len = sb.getBufferLength();
                for (int i = 0; i < len; i++) {
                    baos.write(pcm[i] & 0xFF);
                    baos.write((pcm[i] >> 8) & 0xFF);
                }
                bitstream.closeFrame();
            }
            bitstream.close();
            return new MemoryPcmAudioStream(baos.toByteArray(), sampleRate, channels);
        } catch (Exception e) {
            throw new IOException("MP3 decode failed", e);
        }
    }

    @Override
    public void decodeStreaming(InputStream input, OutputStream output) throws IOException {
        try {
            Bitstream bitstream = new Bitstream(input);
            Decoder decoder = new Decoder();
            while (true) {
                Header h = bitstream.readFrame();
                if (h == null) break;
                SampleBuffer sb = (SampleBuffer) decoder.decodeFrame(h, bitstream);
                short[] pcm = sb.getBuffer();
                int len = sb.getBufferLength();
                for (int i = 0; i < len; i++) {
                    output.write(pcm[i] & 0xFF);
                    output.write((pcm[i] >> 8) & 0xFF);
                }
                bitstream.closeFrame();
            }
            bitstream.close();
        } catch (Exception e) {
            throw new IOException("MP3 streaming failed", e);
        }
    }
}
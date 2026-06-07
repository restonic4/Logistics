package com.restonic4.logistics.audio.codec;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import com.restonic4.logistics.audio.AudioDecoder;
import com.restonic4.logistics.audio.MemoryPcmAudioStream;
import com.restonic4.logistics.audio.PcmAudioStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OggDecoder implements AudioDecoder {
    private static final int BUFFER_SIZE = 4096;

    @Override
    public PcmAudioStream decodeFull(InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Info vi = decodeToOutput(input, baos);
        return new MemoryPcmAudioStream(baos.toByteArray(), vi.rate, vi.channels);
    }

    @Override
    public void decodeStreaming(InputStream input, OutputStream output) throws IOException {
        decodeToOutput(input, output);
    }

    private Info decodeToOutput(InputStream input, OutputStream output) throws IOException {
        SyncState oy = new SyncState();
        StreamState os = new StreamState();
        Info vi = new Info();
        Comment vc = new Comment();
        DspState vd = new DspState();
        Block vb = new Block(vd);
        Page og = new Page();
        Packet op = new Packet();

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytes;
        boolean bos = true;
        int headers = 0;
        int streamSerialNo = -1;

        while ((bytes = input.read(buffer)) > 0) {
            int index = oy.buffer(bytes);
            System.arraycopy(buffer, 0, oy.data, index, bytes);
            oy.wrote(bytes);

            while (oy.pageout(og) == 1) {
                if (bos) {
                    streamSerialNo = og.serialno();
                    os.init(streamSerialNo);
                    vi.init();
                    vc.init();
                    bos = false;
                }

                if (og.serialno() != streamSerialNo) {
                    continue;
                }

                os.pagein(og);

                while (headers < 3) {
                    int result = os.packetout(op);
                    if (result == 0) break;
                    if (result == -1) continue;
                    if (vi.synthesis_headerin(vc, op) >= 0) {
                        headers++;
                    }
                }

                if (headers == 3) {
                    vd.synthesis_init(vi);
                    vb.init(vd);
                    headers = 4;
                }

                if (headers >= 4) {
                    processPackets(os, vd, vb, op, vi, output);
                }
            }
        }

        while (oy.pageout(og) == 1) {
            if (og.serialno() == streamSerialNo) {
                os.pagein(og);
                if (headers >= 4) {
                    processPackets(os, vd, vb, op, vi, output);
                }
            }
        }

        vb.clear();
        vd.clear();
        os.clear();
        oy.clear();

        return vi;
    }

    private void processPackets(StreamState os, DspState vd, Block vb, Packet op, Info vi, OutputStream output) throws IOException {
        float[][][] _pcm = new float[1][][];
        int[] _index = new int[vi.channels];

        while (os.packetout(op) == 1) {
            if (op.e_o_s != 0) {
                return;
            }

            if (vb.synthesis(op) == 0) {
                vd.synthesis_blockin(vb);
            }

            int samples;
            while ((samples = vd.synthesis_pcmout(_pcm, _index)) > 0) {
                float[][] pcm = _pcm[0];
                int len = samples * vi.channels * 2;
                byte[] out = new byte[len];
                int p = 0;

                for (int i = 0; i < samples; i++) {
                    for (int j = 0; j < vi.channels; j++) {
                        int val = (int) (pcm[j][_index[j] + i] * 32767.0f);
                        if (val > 32767) val = 32767;
                        if (val < -32768) val = -32768;
                        out[p++] = (byte) (val & 0xFF);
                        out[p++] = (byte) ((val >> 8) & 0xFF);
                    }
                }

                output.write(out);
                vd.synthesis_read(samples);
            }
        }
    }
}
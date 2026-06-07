package com.restonic4.logistics.audio.stream;

import com.restonic4.logistics.audio.PcmAudioStream;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StreamingAudioSource {
    private static final int BUFFER_COUNT = 6;
    private static final int BUFFER_SIZE = 8192;

    private final int alSource;
    private final int[] alBuffers;
    private final PcmAudioStream stream;
    private final int alFormat;
    private final int sampleRate;
    private final List<Integer> pendingBuffers = new ArrayList<>();
    private boolean initialized = false;
    private boolean finished = false;
    private boolean playing = false;

    public StreamingAudioSource(PcmAudioStream stream) {
        this.stream = stream;
        this.sampleRate = stream.getSampleRate();
        int channels = stream.getChannels();
        this.alFormat = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;

        this.alSource = AL10.alGenSources();
        this.alBuffers = new int[BUFFER_COUNT];
        AL10.alGenBuffers(alBuffers);

        for (int buf : alBuffers) {
            if (!fillBuffer(buf)) break;
        }
        AL10.alSourceQueueBuffers(alSource, alBuffers);
        initialized = true;
    }

    private boolean fillBuffer(int buffer) {
        try {
            byte[] data = new byte[BUFFER_SIZE];
            int read = stream.read(data);
            if (read < 0) return false;
            if (read == 0) return true;

            ByteBuffer b = BufferUtils.createByteBuffer(read);
            b.put(data, 0, read).flip();
            AL10.alBufferData(buffer, alFormat, b, sampleRate);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void tick() {
        if (!initialized || finished) return;

        Iterator<Integer> it = pendingBuffers.iterator();
        while (it.hasNext()) {
            int buf = it.next();
            if (fillBuffer(buf)) {
                AL10.alSourceQueueBuffers(alSource, buf);
                it.remove();
            } else {
                break;
            }
        }

        int processed = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_PROCESSED);
        while (processed-- > 0) {
            int buf = AL10.alSourceUnqueueBuffers(alSource);
            if (fillBuffer(buf)) {
                AL10.alSourceQueueBuffers(alSource, buf);
            } else {
                pendingBuffers.add(buf);
            }
        }

        int state = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE);
        if (state != AL10.AL_PLAYING && !finished) {
            int queued = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_QUEUED);
            if (queued > 0) {
                AL10.alSourcePlay(alSource);
            } else if (pendingBuffers.isEmpty()) {
                finished = true;
            }
        }
    }

    public void updatePosition(Vec3 pos) {
        AL10.alSource3f(alSource, AL10.AL_POSITION, (float) pos.x, (float) pos.y, (float) pos.z);
    }

    public void updateVolume(float volume) {
        AL10.alSourcef(alSource, AL10.AL_GAIN, Math.max(0.0f, volume));
    }

    public void updatePitch(float pitch) {
        AL10.alSourcef(alSource, AL10.AL_PITCH, Math.max(0.01f, pitch));
    }

    public void updateRadius(float radius) {
        AL10.alSourcef(alSource, AL10.AL_REFERENCE_DISTANCE, Math.max(0.1f, radius));
        AL10.alSourcef(alSource, AL10.AL_MAX_DISTANCE, Math.max(0.1f, radius * 2.0f));
        AL10.alSourcef(alSource, AL10.AL_ROLLOFF_FACTOR, 1.0f);
    }

    public void play() {
        playing = true;
        AL10.alSourcePlay(alSource);
    }

    public void stop() {
        playing = false;
        AL10.alSourceStop(alSource);
    }

    public void pause() {
        AL10.alSourcePause(alSource);
    }

    public boolean isFinished() {
        return finished || (!playing && AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE) == AL10.AL_STOPPED && pendingBuffers.isEmpty());
    }

    public void destroy() {
        stop();
        if (initialized) {
            AL10.alSourcei(alSource, AL10.AL_BUFFER, 0);
            AL10.alDeleteBuffers(alBuffers);
            AL10.alDeleteSources(alSource);
        }
        try {
            stream.close();
        } catch (IOException ignored) {}
    }

    public void seek(long ms) {
        AL10.alSourceStop(alSource);
        int queued = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_QUEUED);
        while (queued-- > 0) AL10.alSourceUnqueueBuffers(alSource);
        AL10.alSourceQueueBuffers(alSource, alBuffers);
        pendingBuffers.clear();

        try {
            if (stream.isSeekable()) stream.seek(ms);
        } catch (IOException ignored) {}

        for (int buf : alBuffers) fillBuffer(buf);
        AL10.alSourceQueueBuffers(alSource, alBuffers);
        if (playing) AL10.alSourcePlay(alSource);
    }
}
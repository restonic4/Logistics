package com.restonic4.logistics.audio.stream;

import com.restonic4.logistics.audio.AudioDecoders;
import com.restonic4.logistics.audio.AudioFormat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

public class SharedNetworkStream {
    private final String url;
    private final AudioFormat format;
    private final MultiReaderRingBuffer ringBuffer;
    private final Thread thread;
    private volatile boolean running = false;
    private final AtomicInteger refs = new AtomicInteger(0);
    private volatile int sampleRate = 44100;
    private volatile int channels = 2;

    public SharedNetworkStream(String url, AudioFormat format) {
        this.url = url;
        this.format = format;
        this.ringBuffer = new MultiReaderRingBuffer(1024 * 1024);
        this.thread = new Thread(this::run, "SharedRadio-" + url.hashCode());
        this.thread.setDaemon(true);
    }

    public void start() {
        if (running) return;
        running = true;
        thread.start();
    }

    public void stop() {
        running = false;
        thread.interrupt();
    }

    public void addRef() {
        refs.incrementAndGet();
    }

    public void release() {
        if (refs.decrementAndGet() <= 0) stop();
    }

    public int getRefs() {
        return refs.get();
    }

    public MultiReaderRingBuffer getRingBuffer() {
        return ringBuffer;
    }

    public boolean isRunning() {
        return running;
    }

    public int getSampleRate() { return sampleRate; }
    public int getChannels() { return channels; }

    private void run() {
        try (InputStream net = new URL(url).openStream();
             ByteArrayOutputStream decodeBuffer = new ByteArrayOutputStream()) {
            AudioDecoders.getDecoder(format).decodeStreaming(net, decodeBuffer);
            byte[] chunk = decodeBuffer.toByteArray();
            if (chunk.length > 0) ringBuffer.write(chunk, chunk.length);
        } catch (Exception e) {
            System.err.println("Radio stream failed for " + url + ": " + e.getMessage());
        }

        while (running) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
package com.restonic4.logistics.audio.stream;

import com.restonic4.logistics.audio.AudioFormat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedNetworkStreamManager {
    private static final SharedNetworkStreamManager INSTANCE = new SharedNetworkStreamManager();
    private final Map<String, SharedNetworkStream> streams = new ConcurrentHashMap<>();

    public static SharedNetworkStreamManager getInstance() {
        return INSTANCE;
    }

    public synchronized SharedNetworkStream acquire(String url, AudioFormat format) {
        SharedNetworkStream stream = streams.computeIfAbsent(url, k -> {
            SharedNetworkStream s = new SharedNetworkStream(url, format);
            s.start();
            return s;
        });
        stream.addRef();
        return stream;
    }

    public synchronized void release(String url) {
        SharedNetworkStream stream = streams.get(url);
        if (stream != null) {
            stream.release();
            if (stream.getRefs() <= 0) {
                streams.remove(url);
            }
        }
    }
}
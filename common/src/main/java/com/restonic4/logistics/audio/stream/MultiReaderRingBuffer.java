package com.restonic4.logistics.audio.stream;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class MultiReaderRingBuffer {
    private final byte[] buffer;
    private final int capacity;
    private volatile int writePos = 0;
    private final Map<UUID, Integer> readPositions = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public MultiReaderRingBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new byte[capacity];
    }

    public void registerReader(UUID id) {
        lock.lock();
        try {
            readPositions.put(id, writePos);
        } finally {
            lock.unlock();
        }
    }

    public void unregisterReader(UUID id) {
        readPositions.remove(id);
    }

    public int read(UUID id, byte[] dest) {
        lock.lock();
        try {
            Integer rp = readPositions.get(id);
            if (rp == null) return -1;
            int available = (writePos - rp + capacity) % capacity;
            if (available == 0) return 0;
            int toRead = Math.min(dest.length, available);
            int first = Math.min(toRead, capacity - rp);
            System.arraycopy(buffer, rp, dest, 0, first);
            if (toRead > first) {
                System.arraycopy(buffer, 0, dest, first, toRead - first);
            }
            readPositions.put(id, (rp + toRead) % capacity);
            return toRead;
        } finally {
            lock.unlock();
        }
    }

    public void write(byte[] src, int len) {
        lock.lock();
        try {
            for (int i = 0; i < len; i++) {
                buffer[writePos] = src[i];
                writePos = (writePos + 1) % capacity;
            }
        } finally {
            lock.unlock();
        }
    }
}
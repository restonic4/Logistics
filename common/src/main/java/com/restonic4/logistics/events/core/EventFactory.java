package com.restonic4.logistics.events.core;

import java.util.function.Function;

public class EventFactory {
    /**
     * Creates a standard event where everyone is notified, and return values are ignored.
     */
    public static <T> Event<T> createVoid(Class<T> type, Function<T[], T> implementation) {
        return new Event<>(type, implementation);
    }

    /**
     * Creates a cancellable event.
     */
    public static <T> Event<T> createCancellable(Class<T> type, Function<T[], T> implementation) {
        return new Event<>(type, implementation);
    }
}
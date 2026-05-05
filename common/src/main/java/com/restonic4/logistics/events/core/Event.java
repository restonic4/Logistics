package com.restonic4.logistics.events.core;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Event<T> {
    private final List<T> listeners = new ArrayList<>();
    private final Function<T[], T> invokerFactory;
    private final Class<T> type;

    // Cached invoker to avoid rebuilding on every fire
    private volatile T invoker;

    public Event(Class<T> type, Function<T[], T> invokerFactory) {
        this.type = type;
        this.invokerFactory = invokerFactory;
        this.invoker = invokerFactory.apply(createArray(0));
    }

    public void register(T listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        synchronized (listeners) {
            listeners.add(listener);
            updateInvoker();
        }
    }

    // Returns the invoker.
    public T invoker() {
        return invoker;
    }

    private void updateInvoker() {
        // Create a snapshot of the current listeners
        T[] listenerArray = listeners.toArray(createArray(listeners.size()));
        this.invoker = invokerFactory.apply(listenerArray);
    }

    @SuppressWarnings("unchecked")
    private T[] createArray(int length) {
        return (T[]) Array.newInstance(type, length);
    }
}
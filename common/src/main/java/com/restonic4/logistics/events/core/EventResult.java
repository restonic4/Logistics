package com.restonic4.logistics.events.core;

public enum EventResult {
    PASS, // Continue to the next listener
    CANCEL; // Stop the event propagation immediately
}
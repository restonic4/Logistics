package com.restonic4.logistics.networks.flags;

public enum NetworkFlag implements DirtyFlaggable.DirtyFlag {
    NODE_ADDED(1L),
    NODE_REMOVED(1L << 1),
    MAX_STORAGE_CHANGED(1L << 2),
    STORAGE_CHANGED(1L << 3),
    HAS_DELTA_CHANGES(1L << 4);

    private final long mask;
    NetworkFlag(long mask) { this.mask = mask; }
    @Override public long mask() { return mask; }
}

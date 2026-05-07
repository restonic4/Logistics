package com.restonic4.logistics.networks.flags;

public interface DirtyFlaggable {
    long getDirtyBits();
    void setDirtyBits(long bits);

    default void markDirty(DirtyFlag flag) {
        setDirtyBits(getDirtyBits() | flag.mask());
    }

    default void clearFlag(DirtyFlag flag) {
        setDirtyBits(getDirtyBits() & ~flag.mask());
    }

    default void clearAllFlags() {
        setDirtyBits(0L);
    }

    default boolean isDirty(DirtyFlag flag) {
        return (getDirtyBits() & flag.mask()) != 0;
    }

    default boolean isAnyDirty(DirtyFlag... flags) {
        long combined = 0L;
        for (DirtyFlag f : flags) combined |= f.mask();
        return (getDirtyBits() & combined) != 0;
    }

    interface DirtyFlag {
        long mask();
    }
}

package com.restonic4.logistics.networks.flags;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    default <F extends Enum<F> & DirtyFlag> List<F> getActiveFlags(Class<F> flagClass) {
        return Arrays.stream(flagClass.getEnumConstants())
                .filter(f -> (getDirtyBits() & f.mask()) != 0)
                .collect(Collectors.toList());
    }

    default <F extends Enum<F> & DirtyFlag> String toBinaryString(Class<F> flagClass) {
        long maxMask = Arrays.stream(flagClass.getEnumConstants())
                .mapToLong(DirtyFlag::mask)
                .max()
                .orElse(1L);

        int width = 64 - Long.numberOfLeadingZeros(maxMask);

        return String.format("%" + width + "s", Long.toBinaryString(getDirtyBits())).replace(' ', '0');
    }

    interface DirtyFlag {
        long mask();
    }
}

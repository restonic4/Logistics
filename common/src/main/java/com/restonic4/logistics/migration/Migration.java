package com.restonic4.logistics.migration;

import net.minecraft.nbt.CompoundTag;

public abstract class Migration {
    private final int targetVersion;

    public Migration(int targetVersion) {
        this.targetVersion = targetVersion;
    }

    public int getTargetVersion() {
        return this.targetVersion;
    }

    public void fixItem(CompoundTag rootTag, CompoundTag itemTag, String id) {}
    public void fixBlockEntity(CompoundTag tag, String id) {}
}
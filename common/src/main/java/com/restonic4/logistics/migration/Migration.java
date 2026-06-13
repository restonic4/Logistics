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

    /**
     * Fix an ItemStack tag (held in inventories, entities, parcels in transit, ...).
     * {@code rootTag} is the stack root carrying "id"/"Count"; {@code id} is that "id".
     */
    public void fixItem(CompoundTag rootTag, CompoundTag itemTag, String id) {}

    /**
     * Fix a network node tag stored in the {@code logistics_networks} SavedData.
     * {@code id} is the node type ("type"), which mirrors the owning block's registry id.
     */
    public void fixNode(CompoundTag nodeTag, String id) {}

    /**
     * Fix a block-state palette entry inside a chunk. {@code id} is the block registry id ("Name").
     * Runs while reading the chunk NBT, before vanilla resolves the palette (so a renamed block
     * never falls back to air).
     */
    public void fixBlock(CompoundTag blockStateTag, String id) {}

    // Helpers: rename one registry id to another in-place, returning whether anything changed.

    protected boolean renameItem(CompoundTag rootTag, String from, String to) {
        return rename(rootTag, "id", from, to);
    }

    protected boolean renameNode(CompoundTag nodeTag, String from, String to) {
        return rename(nodeTag, "type", from, to);
    }

    protected boolean renameBlock(CompoundTag blockStateTag, String from, String to) {
        return rename(blockStateTag, "Name", from, to);
    }

    private static boolean rename(CompoundTag tag, String key, String from, String to) {
        if (from.equals(tag.getString(key))) {
            tag.putString(key, to);
            return true;
        }
        return false;
    }
}

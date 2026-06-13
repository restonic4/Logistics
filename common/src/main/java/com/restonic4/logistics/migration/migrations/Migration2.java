package com.restonic4.logistics.migration.migrations;

import com.restonic4.logistics.migration.Migration;
import net.minecraft.nbt.CompoundTag;

public class Migration2 extends Migration {
    private static final String OLD = "logistics:generator";
    private static final String NEW = "logistics:creative_generator";

    public Migration2() {
        super(2);
    }

    @Override
    public void fixItem(CompoundTag rootTag, CompoundTag itemTag, String id) {
        renameItem(rootTag, OLD, NEW);
    }

    @Override
    public void fixNode(CompoundTag nodeTag, String id) {
        renameNode(nodeTag, OLD, NEW);
    }

    @Override
    public void fixBlock(CompoundTag blockStateTag, String id) {
        renameBlock(blockStateTag, OLD, NEW);
    }
}

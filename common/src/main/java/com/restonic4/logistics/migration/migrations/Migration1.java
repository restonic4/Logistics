package com.restonic4.logistics.migration.migrations;

import com.restonic4.logistics.migration.Migration;
import net.minecraft.nbt.CompoundTag;

public class Migration1 extends Migration {
    public Migration1() {
        super(1);
    }

    @Override
    public void fixItem(CompoundTag rootTag, CompoundTag itemTag, String id) {
        if (id.equals("logistics:crystal_shard")) {
            rootTag.putString("id", "logistics:kinetic_crystal_shard");
        }
    }
}

package com.restonic4.logistics.networks.nodes;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FacingNode {
    void setFacing(@NotNull Direction facing);
    @Nullable Direction getFacing();

    default void saveFacing(CompoundTag tag) {
        Direction facing = getFacing();
        if (facing != null) {
            tag.putString("facing", facing.getSerializedName());
        }
    }

    default void loadFacing(CompoundTag tag) {
        if (tag.contains("facing")) {
            Direction facing = Direction.byName(tag.getString("facing"));
            if (facing != null) {
                setFacing(facing);
            }
        }
    }
}

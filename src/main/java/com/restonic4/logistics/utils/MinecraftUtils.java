package com.restonic4.logistics.utils;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.function.Consumer;

public class MinecraftUtils {
    private static List<BlockPos> CARDINAL_NEIGHBORS_CACHE;

    public static void forEachNeighbor(BlockPos pos, Consumer<BlockPos> action) {
        if (CARDINAL_NEIGHBORS_CACHE.isEmpty()) {
            CARDINAL_NEIGHBORS_CACHE = List.of(
                    pos.north(), pos.south(),
                    pos.east(), pos.west(),
                    pos.above(), pos.below()
            );
        }

        for (BlockPos blockPos : CARDINAL_NEIGHBORS_CACHE) {
            action.accept(blockPos);
        }
    }
}

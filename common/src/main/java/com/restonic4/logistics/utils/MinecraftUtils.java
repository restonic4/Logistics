package com.restonic4.logistics.utils;

import com.restonic4.logistics.networks.Network;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class MinecraftUtils {
    private static List<BlockPos> CARDINAL_NEIGHBORS_CACHE;

    // TODO: what
    public static void forEachNeighbor(BlockPos pos, Consumer<BlockPos> action) {
        //if (CARDINAL_NEIGHBORS_CACHE == null) {
            CARDINAL_NEIGHBORS_CACHE = List.of(
                    pos.north(), pos.south(),
                    pos.east(), pos.west(),
                    pos.above(), pos.below()
            );
        //}

        for (BlockPos blockPos : CARDINAL_NEIGHBORS_CACHE) {
            action.accept(blockPos);
        }
    }

    public static <T> Optional<T> findNeighbor(BlockPos pos, Function<BlockPos, @Nullable T> mapper) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            T result = mapper.apply(neighbor);
            if (result != null) return Optional.of(result);
        }
        return Optional.empty();
    }
}

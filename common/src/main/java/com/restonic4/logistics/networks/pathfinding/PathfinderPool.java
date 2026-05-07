package com.restonic4.logistics.networks.pathfinding;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public class PathfinderPool {
    private final ThreadLocal<Pathfinder> pool;

    public PathfinderPool(Pathfinder.NodeProvider nodeProvider) {
        this.pool = ThreadLocal.withInitial(() -> new Pathfinder(nodeProvider));
    }

    @Nullable
    public BlockPos[] findPath(BlockPos start, BlockPos end) {
        return pool.get().findPath(start, end);
    }
}

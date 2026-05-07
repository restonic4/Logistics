package com.restonic4.logistics.networks.pathfinding;

import com.restonic4.logistics.Constants;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Pathfinder {
    private static final long[] DIRECTION_DELTAS = new long[6];

    static {
        Direction[] dirs = Direction.values();
        BlockPos origin = BlockPos.ZERO;
        long originL = origin.asLong();
        for (int i = 0; i < dirs.length; i++) {
            BlockPos neighbor = origin.relative(dirs[i]);
            DIRECTION_DELTAS[i] = neighbor.asLong() - originL;
        }
    }

    private static final byte VISITED_FORWARD = 1;
    private static final byte VISITED_BACKWARD = 2;

    private static final int QUEUE_CAPACITY = Constants.MAX_NODES_PER_NETWORK;
    private static final int QUEUE_MASK = QUEUE_CAPACITY - 1;

    private final long[] fwdQueue = new long[QUEUE_CAPACITY];
    private final long[] bwdQueue = new long[QUEUE_CAPACITY];

    private final Long2ByteOpenHashMap fwdVisited = new Long2ByteOpenHashMap(1024, 0.5f);
    private final Long2ByteOpenHashMap bwdVisited = new Long2ByteOpenHashMap(1024, 0.5f);

    private final Long2LongOpenHashMap fwdParent = new Long2LongOpenHashMap(1024, 0.5f);
    private final Long2LongOpenHashMap bwdParent = new Long2LongOpenHashMap(1024, 0.5f);

    private long[] resultBuffer = new long[512];

    private final NodeProvider nodeProvider;

    public Pathfinder(NodeProvider nodeProvider) {
        this.nodeProvider = nodeProvider;
    }

    @Nullable
    public BlockPos[] findPath(BlockPos start, BlockPos end) {
        long startL = start.asLong();
        long endL = end.asLong();

        if (startL == endL) {
            return new BlockPos[]{ start };
        }

        // Reset state
        fwdVisited.clear();
        bwdVisited.clear();
        fwdParent.clear();
        bwdParent.clear();

        // Seed both frontiers
        int fwdHead = 0, fwdTail = 0;
        int bwdHead = 0, bwdTail = 0;

        fwdQueue[fwdTail++ & QUEUE_MASK] = startL;
        fwdVisited.put(startL, VISITED_FORWARD);

        bwdQueue[bwdTail++ & QUEUE_MASK] = endL;
        bwdVisited.put(endL, VISITED_BACKWARD);

        long meetingPoint = Long.MIN_VALUE;

        // Main BFS loop
        outer:
        while (fwdHead != fwdTail || bwdHead != bwdTail) {

            // Expand forward frontier one level
            if (fwdHead != fwdTail) {
                int levelEnd = fwdTail; // snapshot current tail, process one level
                while (fwdHead != levelEnd) {
                    long current = fwdQueue[fwdHead++ & QUEUE_MASK];

                    for (long delta : DIRECTION_DELTAS) {
                        long neighbor = current + delta;

                        // Skip if already visited from this direction
                        if (fwdVisited.containsKey(neighbor)) continue;

                        // Skip if not a valid pipe node
                        if (!nodeProvider.isPassable(neighbor)) continue;

                        fwdVisited.put(neighbor, VISITED_FORWARD);
                        fwdParent.put(neighbor, current);

                        // Check for intersection with backward frontier
                        if (bwdVisited.containsKey(neighbor)) {
                            meetingPoint = neighbor;
                            break outer;
                        }

                        // Overflow guard, should never happen in well-sized networks
                        if (((fwdTail - fwdHead) & QUEUE_MASK) >= QUEUE_CAPACITY - 1) {
                            // Queue full; path likely exceeds network size assumption
                            return null;
                        }
                        fwdQueue[fwdTail++ & QUEUE_MASK] = neighbor;
                    }
                }
            }

            // Expand backward frontier one level
            if (bwdHead != bwdTail) {
                int levelEnd = bwdTail;
                while (bwdHead != levelEnd) {
                    long current = bwdQueue[bwdHead++ & QUEUE_MASK];

                    for (long delta : DIRECTION_DELTAS) {
                        long neighbor = current + delta;

                        if (bwdVisited.containsKey(neighbor)) continue;
                        if (!nodeProvider.isPassable(neighbor)) continue;

                        bwdVisited.put(neighbor, VISITED_BACKWARD);
                        bwdParent.put(neighbor, current);

                        if (fwdVisited.containsKey(neighbor)) {
                            meetingPoint = neighbor;
                            break outer;
                        }

                        if (((bwdTail - bwdHead) & QUEUE_MASK) >= QUEUE_CAPACITY - 1) {
                            return null;
                        }
                        bwdQueue[bwdTail++ & QUEUE_MASK] = neighbor;
                    }
                }
            }
        }

        if (meetingPoint == Long.MIN_VALUE) {
            return null; // No path found
        }

        return reconstructPath(startL, endL, meetingPoint);
    }

    @Nullable
    private BlockPos[] reconstructPath(long startL, long endL, long meetingPoint) {
        // Count forward segment length (meetingPoint -> start)
        int fwdLen = 0;
        long cur = meetingPoint;
        while (cur != startL) {
            fwdLen++;
            if (!fwdParent.containsKey(cur)) return null; // shouldn't happen
            cur = fwdParent.get(cur);
        }
        // fwdLen = number of hops from start to meetingPoint (not counting start itself)

        // Count backward segment length (meetingPoint -> end)
        int bwdLen = 0;
        cur = meetingPoint;
        while (cur != endL) {
            bwdLen++;
            if (!bwdParent.containsKey(cur)) return null;
            cur = bwdParent.get(cur);
        }

        int totalLen = fwdLen + bwdLen + 1; // +1 for meetingPoint itself

        // Grow result buffer if needed (rare)
        if (totalLen > resultBuffer.length) {
            resultBuffer = new long[Integer.highestOneBit(totalLen) << 1];
        }

        // Fill forward segment into buffer (reversed, so start is at [0]) - resultBuffer[0] = start, resultBuffer[fwdLen] = meetingPoint
        cur = meetingPoint;
        for (int i = fwdLen; i >= 1; i--) {
            resultBuffer[i] = cur;
            cur = fwdParent.get(cur);
        }
        resultBuffer[0] = startL;

        // Fill backward segment (meetingPoint+1 .. end)
        cur = meetingPoint;
        for (int i = fwdLen + 1; i < totalLen; i++) {
            cur = bwdParent.get(cur);
            resultBuffer[i] = cur;
        }

        // Convert to BlockPos[]
        BlockPos[] path = new BlockPos[totalLen];
        for (int i = 0; i < totalLen; i++) {
            path[i] = BlockPos.of(resultBuffer[i]);
        }
        return path;
    }

    @FunctionalInterface
    public interface NodeProvider {
        boolean isPassable(long packedPos);
    }

    public static void demo() {
        Set<Long> nodes = ConcurrentHashMap.newKeySet();

        BlockPos a = new BlockPos(0, 64, 0);
        BlockPos b = new BlockPos(10, 64, 0);
        for (int x = 0; x <= 10; x++) {
            nodes.add(new BlockPos(x, 64, 0).asLong());
        }

        PathfinderPool pathfinderPool = new PathfinderPool(nodes::contains);

        BlockPos[] path = pathfinderPool.findPath(a, b);
        if (path != null) {
            System.out.println("Path length: " + path.length); // 11
            for (BlockPos step : path) {
                System.out.println("  " + step);
            }
        }
    }

    public static void main(String[] args) {
        demo();
    }
}

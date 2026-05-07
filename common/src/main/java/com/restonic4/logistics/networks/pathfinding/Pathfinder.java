package com.restonic4.logistics.networks.pathfinding;

import com.restonic4.logistics.Constants;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Pathfinder {
    private static final int[] DX = new int[6];
    private static final int[] DY = new int[6];
    private static final int[] DZ = new int[6];

    private static final int MAX_SEARCH_DEPTH = 10000;

    static {
        Direction[] dirs = Direction.values();
        for (int i = 0; i < dirs.length; i++) {
            DX[i] = dirs[i].getStepX();
            DY[i] = dirs[i].getStepY();
            DZ[i] = dirs[i].getStepZ();
        }
    }

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

        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        Long2LongOpenHashMap cameFrom = new Long2LongOpenHashMap();

        queue.enqueue(startL);
        cameFrom.put(startL, startL);

        boolean found = false;

        while (!queue.isEmpty()) {
            if (cameFrom.size() > MAX_SEARCH_DEPTH) break;

            long currentL = queue.dequeueLong();

            if (currentL == endL) {
                found = true;
                break;
            }

            int cx = BlockPos.getX(currentL);
            int cy = BlockPos.getY(currentL);
            int cz = BlockPos.getZ(currentL);

            for (int i = 0; i < 6; i++) {
                long neighborL = BlockPos.asLong(cx + DX[i], cy + DY[i], cz + DZ[i]);

                if (!cameFrom.containsKey(neighborL) && nodeProvider.isPassable(neighborL)) {
                    queue.enqueue(neighborL);
                    cameFrom.put(neighborL, currentL);
                }
            }
        }

        if (!found) {
            return null;
        }

        LongArrayList pathPacked = new LongArrayList();
        long curr = endL;

        while (curr != startL) {
            pathPacked.add(curr);
            curr = cameFrom.get(curr);
        }
        pathPacked.add(startL);

        BlockPos[] path = new BlockPos[pathPacked.size()];
        for (int i = 0; i < pathPacked.size(); i++) {
            path[i] = BlockPos.of(pathPacked.getLong(pathPacked.size() - 1 - i));
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

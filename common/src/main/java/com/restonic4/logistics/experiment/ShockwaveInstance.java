package com.restonic4.logistics.experiment;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ShockwaveInstance {
    private static final int FACE_DOWN  = 0x01;
    private static final int FACE_UP    = 0x02;
    private static final int FACE_NORTH = 0x04;
    private static final int FACE_SOUTH = 0x08;
    private static final int FACE_WEST  = 0x10;
    private static final int FACE_EAST  = 0x20;

    private final BlockPos origin;
    private final double maxRadius;
    private final double thickness;
    private final double expansionDuration;
    private final double fadeOutDuration;
    private final int baseColorARGB;
    private final double scanIntensityMs;

    private long startTime;
    private boolean activated = false;
    private boolean expired = false;
    private volatile boolean chunkQueueReady = false;

    private final LongArrayList[] posBuckets;
    private final ByteArrayList[] maskBuckets;

    private final LongArrayList pendingChunks;
    private int scanIndex;

    private final LongArrayList framePositions;
    private final ByteArrayList frameMasks;
    private final BatchBlockRenderer renderer;

    private final Long2BooleanOpenHashMap sectionCache = new Long2BooleanOpenHashMap();
    private final AABB reusableBlockAABB = new AABB(0, 0, 0, 0, 0, 0);
    private final AABB reusableSectionAABB = new AABB(0, 0, 0, 0, 0, 0);

    private final long creationTime = System.currentTimeMillis();
    private java.util.function.Consumer<Long> onLoadCallback = null;
    private boolean loadNotificationTriggered = false;

    public ShockwaveInstance(ClientLevel level, BlockPos origin, double maxRadius, double thickness,
                             double expansionDuration, double fadeOutDuration, int colorARGB, double scanIntensityMs) {
        this.origin = origin.immutable();
        this.maxRadius = maxRadius;
        this.thickness = thickness;
        this.expansionDuration = expansionDuration;
        this.fadeOutDuration = fadeOutDuration;
        this.baseColorARGB = colorARGB;
        this.scanIntensityMs = scanIntensityMs;

        int bucketCount = (int) (maxRadius + thickness * 0.5) + 2;
        this.posBuckets = new LongArrayList[bucketCount];
        this.maskBuckets = new ByteArrayList[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            this.posBuckets[i] = new LongArrayList();
            this.maskBuckets[i] = new ByteArrayList();
        }

        this.framePositions = new LongArrayList();
        this.frameMasks = new ByteArrayList();
        this.renderer = new BatchBlockRenderer(colorARGB);
        this.pendingChunks = new LongArrayList();

        // Pipeline async layout initialization, running calculations entirely multi-threaded
        CompletableFuture.supplyAsync(this::buildChunkQueueInternal)
                .thenAcceptAsync(queue -> {
                    this.pendingChunks.addAll(queue);

                    // Main thread safe section snapshotted references
                    List<ChunkSnapshot> snapshots = new ArrayList<>();
                    for (int i = 0; i < queue.size(); i++) {
                        long chunkPacked = queue.getLong(i);
                        int cx = (int) (chunkPacked);
                        int cz = (int) (chunkPacked >> 32);

                        LevelChunk chunk = level.getChunk(cx, cz);
                        if (chunk == null) continue;

                        LevelChunk chunkWest  = level.getChunk(cx - 1, cz);
                        LevelChunk chunkEast  = level.getChunk(cx + 1, cz);
                        LevelChunk chunkNorth = level.getChunk(cx, cz - 1);
                        LevelChunk chunkSouth = level.getChunk(cx, cz + 1);

                        snapshots.add(new ChunkSnapshot(cx, cz, chunk, chunkWest, chunkEast, chunkNorth, chunkSouth));
                    }

                    // Process all chunks asynchronously across multiple CPU worker cores completely off-thread
                    CompletableFuture.supplyAsync(() -> snapshots.parallelStream()
                            .map(snapshot -> scanChunkSnapshot(snapshot, this.origin, this.maxRadius, this.thickness, bucketCount))
                            .toList()
                    ).thenAcceptAsync(results -> {
                        // Safely merge thread-local buckets back into main arrays on the main thread
                        for (ChunkResult res : results) {
                            for (int b = 0; b < bucketCount; b++) {
                                this.posBuckets[b].addAll(res.posBuckets[b]);
                                this.maskBuckets[b].addAll(res.maskBuckets[b]);
                            }
                        }

                        this.scanIndex = this.pendingChunks.size();
                        this.chunkQueueReady = true;

                        if (!loadNotificationTriggered) {
                            loadNotificationTriggered = true;
                            if (onLoadCallback != null) {
                                onLoadCallback.accept(System.currentTimeMillis() - creationTime);
                            }
                        }
                    }, Minecraft.getInstance());

                }, Minecraft.getInstance());
    }

    /**
     * Heavy processing execution worker per chunk. Utilizes a padded 18x18x18 grid array cache
     * to eliminate redudant block-state palette reads.
     */
    private static ChunkResult scanChunkSnapshot(ChunkSnapshot snapshot, BlockPos origin, double maxRadius, double thickness, int bucketCount) {
        ChunkResult result = new ChunkResult(bucketCount);
        double maxAllowedDist = maxRadius + thickness * 0.5 + 2.0;
        double maxAllowedDistSq = maxAllowedDist * maxAllowedDist;

        LevelChunkSection[] sections = snapshot.sections;
        int sectionCount = sections.length;
        int minSection = snapshot.minSection;

        double originX = origin.getX() + 0.5;
        double originY = origin.getY() + 0.5;
        double originZ = origin.getZ() + 0.5;

        int worldX = snapshot.cx << 4;
        int worldZ = snapshot.cz << 4;

        // Allocated once per chunk task to avoid GC allocation thrashing
        boolean[] airGrid = new boolean[5832];

        for (int i = 0; i < sectionCount; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) continue;

            int baseY = (minSection + i) << 4;

            // Bounding box early skip optimization
            int secCenterX = worldX + 8;
            int secCenterY = baseY + 8;
            int secCenterZ = worldZ + 8;
            int dx = Math.max(0, Math.abs(secCenterX - origin.getX()) - 8);
            int dy = Math.max(0, Math.abs(secCenterY - origin.getY()) - 8);
            int dz = Math.max(0, Math.abs(secCenterZ - origin.getZ()) - 8);
            double minDistSq = (double) dx * dx + (double) dy * dy + (double) dz * dz;
            if (minDistSq > maxAllowedDistSq) continue;

            // Phase 1: Flat-populate core air grid positions
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        airGrid[(x + 1) + (y + 1) * 18 + (z + 1) * 324] = section.getBlockState(x, y, z).isAir();
                    }
                }
            }

            // Extract neighboring boundaries
            LevelChunkSection secBelow = (i > 0) ? sections[i - 1] : null;
            LevelChunkSection secAbove = (i < sectionCount - 1) ? sections[i + 1] : null;
            LevelChunkSection secWest  = (snapshot.westSections  != null && i < snapshot.westSections.length)  ? snapshot.westSections[i]  : null;
            LevelChunkSection secEast  = (snapshot.eastSections  != null && i < snapshot.eastSections.length)  ? snapshot.eastSections[i]  : null;
            LevelChunkSection secNorth = (snapshot.northSections != null && i < snapshot.northSections.length) ? snapshot.northSections[i] : null;
            LevelChunkSection secSouth = (snapshot.southSections != null && i < snapshot.southSections.length) ? snapshot.southSections[i] : null;

            // Shell boundaries filling (0 and 17 coordinates offsets)
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    airGrid[(x + 1) + 0 * 18 + (z + 1) * 324] = secBelow != null && secBelow.getBlockState(x, 15, z).isAir();
                    airGrid[(x + 1) + 17 * 18 + (z + 1) * 324] = secAbove != null && secAbove.getBlockState(x, 0, z).isAir();
                }
            }
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    airGrid[(x + 1) + (y + 1) * 18 + 0 * 324] = secNorth != null && secNorth.getBlockState(x, y, 15).isAir();
                    airGrid[(x + 1) + (y + 1) * 18 + 17 * 324] = secSouth != null && secSouth.getBlockState(x, y, 0).isAir();
                }
            }
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    airGrid[0 + (y + 1) * 18 + (z + 1) * 324] = secWest != null && secWest.getBlockState(15, y, z).isAir();
                    airGrid[17 + (y + 1) * 18 + (z + 1) * 324] = secEast != null && secEast.getBlockState(0, y, z).isAir();
                }
            }

            // Phase 2: Compute face masks utilizing array index lookups instead of container calls
            for (int y = 0; y < 16; y++) {
                int wy = baseY + y;
                double ddy = wy + 0.5 - originY;
                for (int z = 0; z < 16; z++) {
                    int wz = worldZ + z;
                    double ddz = wz + 0.5 - originZ;
                    for (int x = 0; x < 16; x++) {
                        int idx = (x + 1) + (y + 1) * 18 + (z + 1) * 324;
                        if (airGrid[idx]) continue;

                        int wx = worldX + x;
                        double ddx = wx + 0.5 - originX;
                        double distSq = ddx * ddx + ddy * ddy + ddz * ddz;

                        if (distSq > maxAllowedDistSq) continue;

                        byte mask = 0;
                        if (airGrid[idx - 18])  mask |= FACE_DOWN;
                        if (airGrid[idx + 18])  mask |= FACE_UP;
                        if (airGrid[idx - 324]) mask |= FACE_NORTH;
                        if (airGrid[idx + 324]) mask |= FACE_SOUTH;
                        if (airGrid[idx - 1])   mask |= FACE_WEST;
                        if (airGrid[idx + 1])   mask |= FACE_EAST;

                        if (mask != 0) {
                            double dist = Math.sqrt(distSq);
                            int bucket = Math.min((int) dist, bucketCount - 1);
                            result.posBuckets[bucket].add(BlockPos.asLong(wx, wy, wz));
                            result.maskBuckets[bucket].add(mask);
                        }
                    }
                }
            }
        }
        return result;
    }

    public void setOnLoadCallback(java.util.function.Consumer<Long> callback) {
        this.onLoadCallback = callback;
        if (isScanComplete() && !loadNotificationTriggered && onLoadCallback != null) {
            loadNotificationTriggered = true;
            onLoadCallback.accept(System.currentTimeMillis() - creationTime);
        }
    }

    public double getScanIntensityMs() {
        return this.scanIntensityMs;
    }

    public void activate() {
        if (this.activated) return;
        this.startTime = System.currentTimeMillis();
        this.activated = true;
    }

    public boolean isActivated() {
        return this.activated;
    }

    public boolean isScanComplete() {
        return this.chunkQueueReady && this.scanIndex >= this.pendingChunks.size();
    }

    @Deprecated
    public void tickScan(ClientLevel level, long maxNanoBudget) {
        // Main thread budget allocation is no longer needed since it scans multi-threaded automatically
    }

    public void tickAndRender(PoseStack poseStack, double camX, double camY, double camZ, Frustum frustum) {
        if (!this.activated) return;

        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;

        if (elapsed >= expansionDuration + fadeOutDuration) {
            this.expired = true;
            return;
        }

        double currentRadius = (elapsed <= expansionDuration)
                ? (elapsed / expansionDuration) * maxRadius
                : maxRadius;

        float alpha = 1.0f;
        double fadeStart = Math.max(0.0, expansionDuration - fadeOutDuration);
        if (elapsed >= fadeStart) {
            alpha = 1.0f - (float) ((elapsed - fadeStart) / fadeOutDuration);
            alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        }

        if (alpha <= 0.01f) {
            this.expired = true;
            return;
        }

        int baseAlpha = (baseColorARGB >> 24) & 0xFF;
        if (baseAlpha == 0) baseAlpha = 255;
        int dynamicAlpha = (int) (baseAlpha * alpha);
        int dynamicColor = (dynamicAlpha << 24) | (baseColorARGB & 0x00FFFFFF);
        renderer.setColor(dynamicColor);

        double inner = currentRadius - (thickness * 0.5);
        double outer = currentRadius + (thickness * 0.5);
        int minBucket = Math.max(0, (int) (inner - 1.0));
        int maxBucket = Math.min(posBuckets.length - 1, (int) (outer + 1.0));

        framePositions.clear();
        frameMasks.clear();
        sectionCache.clear();

        for (int b = minBucket; b <= maxBucket; b++) {
            LongArrayList positions = posBuckets[b];
            ByteArrayList masks = maskBuckets[b];
            int size = positions.size();

            for (int i = 0; i < size; i++) {
                long packed = positions.getLong(i);
                int x = BlockPos.getX(packed);
                int y = BlockPos.getY(packed);
                int z = BlockPos.getZ(packed);

                int sx = x >> 4;
                int sy = y >> 4;
                int sz = z >> 4;

                long sectionKey = ((long) (sx & 0x3FFFFFF) << 38) | ((long) (sy & 0xFFF) << 26) | ((long) (sz & 0x3FFFFFF));

                boolean sectionVisible;
                if (sectionCache.containsKey(sectionKey)) {
                    sectionVisible = sectionCache.get(sectionKey);
                } else {
                    int minX = sx << 4;
                    int minY = sy << 4;
                    int minZ = sz << 4;

                    AABB secAABB = new AABB(minX, minY, minZ, minX + 16, minY + 16, minZ + 16);

                    sectionVisible = frustum.isVisible(secAABB);
                    sectionCache.put(sectionKey, sectionVisible);
                }

                if (!sectionVisible) continue;

                AABB blockAABB = new AABB(x, y, z, x + 1, y + 1, z + 1);

                if (!frustum.isVisible(blockAABB)) continue;

                framePositions.add(packed);
                frameMasks.add(masks.getByte(i));
            }
        }

        if (!framePositions.isEmpty()) {
            renderer.render(poseStack, camX, camY, camZ, framePositions, frameMasks);
        }
    }

    public boolean isExpired() {
        if (!this.activated) return false;
        return expired;
    }

    private LongArrayList buildChunkQueueInternal() {
        int chunkRadius = (int) ((maxRadius + thickness * 0.5 + 16.0) / 16.0) + 1;
        int originCX = origin.getX() >> 4;
        int originCZ = origin.getZ() >> 4;

        LongArrayList chunks = new LongArrayList();
        DoubleArrayList distances = new DoubleArrayList();

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int cx = originCX + dx;
                int cz = originCZ + dz;

                double dX = (cx << 4) + 8.0 - (origin.getX() + 0.5);
                double dZ = (cz << 4) + 8.0 - (origin.getZ() + 0.5);
                double dist = Math.sqrt(dX * dX + dZ * dZ);

                if (dist <= maxRadius + thickness * 0.5 + 24.0) {
                    chunks.add(((long) cz << 32) | (cx & 0xFFFFFFFFL));
                    distances.add(dist);
                }
            }
        }

        quickSortParallel(chunks.elements(), distances.elements(), 0, chunks.size() - 1);
        return chunks;
    }

    private static void quickSortParallel(long[] keys, double[] values, int left, int right) {
        while (left < right) {
            int i = left, j = right;
            double pivot = values[(left + right) >>> 1];
            while (i <= j) {
                while (values[i] < pivot) i++;
                while (values[j] > pivot) j--;
                if (i <= j) {
                    long tmpK = keys[i]; keys[i] = keys[j]; keys[j] = tmpK;
                    double tmpV = values[i]; values[i] = values[j]; values[j] = tmpV;
                    i++; j--;
                }
            }
            if (j - left < right - i) {
                if (j > left) quickSortParallel(keys, values, left, j);
                left = i;
            } else {
                if (i < right) quickSortParallel(keys, values, i, right);
                right = j;
            }
        }
    }

    // Thread-safe immutable holder structures for background worker transfers
    private static class ChunkSnapshot {
        final int cx, cz;
        final int minSection;
        final LevelChunkSection[] sections;
        final LevelChunkSection[] westSections;
        final LevelChunkSection[] eastSections;
        final LevelChunkSection[] northSections;
        final LevelChunkSection[] southSections;

        ChunkSnapshot(int cx, int cz, LevelChunk chunk, LevelChunk west, LevelChunk east, LevelChunk north, LevelChunk south) {
            this.cx = cx;
            this.cz = cz;
            this.minSection = chunk.getMinSection();
            this.sections = chunk.getSections();
            this.westSections = west != null ? west.getSections() : null;
            this.eastSections = east != null ? east.getSections() : null;
            this.northSections = north != null ? north.getSections() : null;
            this.southSections = south != null ? south.getSections() : null;
        }
    }

    private static class ChunkResult {
        final LongArrayList[] posBuckets;
        final ByteArrayList[] maskBuckets;

        ChunkResult(int bucketCount) {
            this.posBuckets = new LongArrayList[bucketCount];
            this.maskBuckets = new ByteArrayList[bucketCount];
            for (int i = 0; i < bucketCount; i++) {
                this.posBuckets[i] = new LongArrayList();
                this.maskBuckets[i] = new ByteArrayList();
            }
        }
    }
}
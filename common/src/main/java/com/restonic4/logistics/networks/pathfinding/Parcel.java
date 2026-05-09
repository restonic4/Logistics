package com.restonic4.logistics.networks.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class Parcel {
    private ItemStack itemStack;
    private BlockPos start;
    private BlockPos end;
    private BlockPos[] path;
    private double timePerBlock;

    private long startTime;
    private long durationMillis;

    private Parcel() {}

    public Parcel(ItemStack itemStack, BlockPos start, BlockPos end, double timePerBlock) {
        this.itemStack = itemStack;
        this.start = start;
        this.end = end;
        this.timePerBlock = timePerBlock;
    }

    public static Parcel fromCompoundTag(CompoundTag tag) {
        Parcel parcel = new Parcel();
        parcel.load(tag);
        return parcel;
    }

    public void recalculate(Set<Long> nodes, BlockPos start, double timePerBlock) {
        PathfinderPool pathfinderPool = new PathfinderPool(nodes::contains);
        this.start = start;
        this.timePerBlock = timePerBlock;
        this.path = pathfinderPool.findPath(this.start, this.end);

        this.startTime = System.currentTimeMillis();
        this.durationMillis = (path != null && path.length > 0) ? (long)((path.length - 1) * this.timePerBlock) : 0;
    }

    public void recalculate(Set<Long> nodes) {
        this.recalculate(nodes, start, timePerBlock);
    }

    public Vec3 getPosition() {
        if (path == null || path.length == 0) return new Vec3(start.getX(), start.getY(), start.getZ());
        if (path.length == 1) return new Vec3(path[0].getX(), path[0].getY(), path[0].getZ());

        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed >= durationMillis) {
            BlockPos last = path[path.length - 1];
            return new Vec3(last.getX(), last.getY(), last.getZ());
        }
        if (elapsed <= 0) {
            return new Vec3(path[0].getX(), path[0].getY(), path[0].getZ());
        }

        // Determine which segment we are currently on
        double totalSeconds = elapsed / this.timePerBlock;
        int segmentIndex = (int) Math.floor(totalSeconds);
        double segmentProgress = totalSeconds - segmentIndex;

        if (segmentIndex >= path.length - 1) {
            BlockPos last = path[path.length - 1];
            return new Vec3(last.getX(), last.getY(), last.getZ());
        }

        BlockPos p1 = path[segmentIndex];
        BlockPos p2 = path[segmentIndex + 1];

        return lerp(p1, p2, segmentProgress);
    }

    private Vec3 lerp(BlockPos a, BlockPos b, double t) {
        double x = a.getX() + t * (b.getX() - a.getX());
        double y = a.getY() + t * (b.getY() - a.getY());
        double z = a.getZ() + t * (b.getZ() - a.getZ());
        return new Vec3(x, y, z);
    }

    public BlockPos[] getPath() {
        return this.path;
    }

    public ItemStack getItemStackClone() {
        return itemStack.copy();
    }

    public boolean isEmpty() {
        return itemStack == null || itemStack.isEmpty();
    }

    public int getCount() {
        return itemStack.getCount();
    }

    public int getMaxStackSize() {
        return itemStack.getMaxStackSize();
    }

    public void shrink(int amount) {
        this.itemStack.shrink(amount);
    }

    public int growSafely(int amount) {
        int spaceLeft = itemStack.getMaxStackSize() - itemStack.getCount();
        int toAdd = Math.min(amount, spaceLeft);
        itemStack.grow(toAdd);
        return toAdd;
    }

    public void growDangerously(int amount) {
        itemStack.grow(amount);
    }

    public long getTimeLeft() {
        long elapsed = System.currentTimeMillis() - this.startTime;
        long remaining = this.durationMillis - elapsed;

        return Math.max(0, remaining);
    }

    public boolean isFinished() {
        return getTimeLeft() <= 0;
    }

    public double getTimePerBlock() {
        return timePerBlock;
    }

    public final CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.put("itemStack", itemStack.save(new CompoundTag()));
        tag.put("start", NbtUtils.writeBlockPos(start));
        tag.put("end", NbtUtils.writeBlockPos(end));
        tag.putDouble("timePerBlock", timePerBlock);
        tag.putLong("durationMillis", durationMillis);

        long elapsedSoFar = System.currentTimeMillis() - startTime;
        tag.putLong("elapsedSoFar", elapsedSoFar);

        if (path != null) {
            ListTag pathTag = new ListTag();
            for (BlockPos pos : path) {
                pathTag.add(NbtUtils.writeBlockPos(pos));
            }
            tag.put("path", pathTag);
        }

        return tag;
    }

    public final void load(CompoundTag tag) {
        this.itemStack = ItemStack.of(tag.getCompound("itemStack"));
        this.start = NbtUtils.readBlockPos(tag.getCompound("start"));
        this.end = NbtUtils.readBlockPos(tag.getCompound("end"));
        this.timePerBlock = tag.getDouble("timePerBlock");
        this.durationMillis = tag.getLong("durationMillis");

        long elapsedSoFar = tag.getLong("elapsedSoFar");
        this.startTime = System.currentTimeMillis() - elapsedSoFar;

        if (tag.contains("path", Tag.TAG_LIST)) {
            ListTag pathTag = tag.getList("path", Tag.TAG_COMPOUND);
            this.path = new BlockPos[pathTag.size()];
            for (int i = 0; i < pathTag.size(); i++) {
                this.path[i] = NbtUtils.readBlockPos(pathTag.getCompound(i));
            }
        }
    }

    public BlockPos getStartPos() {
        return start;
    }

    public BlockPos getEndPos() {
        return end;
    }
}

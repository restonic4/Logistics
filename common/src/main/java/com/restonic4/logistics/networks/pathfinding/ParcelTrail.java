package com.restonic4.logistics.networks.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * A queue of already-extracted stacks waiting to be shipped from one node to another,
 * one {@link Parcel} every {@link #DISPATCH_INTERVAL_MS}. This is how transfers larger
 * than a single parcel (more than one stack, or unstackable NBT variants) travel: the
 * first parcel ships immediately, the rest follow as a trail.
 * <p>
 * The pending items are owned by the trail (they were removed from the source container
 * when the transfer was requested), so on save/split they must either keep travelling or
 * be dropped into the world — never silently discarded.
 */
public class ParcelTrail {
    public static final long DISPATCH_INTERVAL_MS = 500L;

    private BlockPos start;
    private BlockPos end;
    private final Deque<ItemStack> pending = new ArrayDeque<>();
    /** Absolute wall-clock time of the next dispatch; persisted as a relative delay. */
    private long nextDispatchTime;

    private ParcelTrail() {}

    public ParcelTrail(BlockPos start, BlockPos end, Collection<ItemStack> stacks) {
        this.start = start;
        this.end = end;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) pending.addLast(stack);
        }
        this.nextDispatchTime = System.currentTimeMillis() + DISPATCH_INTERVAL_MS;
    }

    public BlockPos getStartPos() { return start; }
    public BlockPos getEndPos() { return end; }

    public boolean isFinished() { return pending.isEmpty(); }
    public int getPendingParcelCount() { return pending.size(); }

    public int getPendingItemCount() {
        int total = 0;
        for (ItemStack stack : pending) total += stack.getCount();
        return total;
    }

    /** A read-only view of the queued stacks (e.g. for in-flight accounting). */
    public Collection<ItemStack> getPending() {
        return Collections.unmodifiableCollection(pending);
    }

    /** Removes and returns every queued stack (e.g. to drop them when the trail loses its network). */
    public List<ItemStack> drainPending() {
        List<ItemStack> drained = new ArrayList<>(pending);
        pending.clear();
        return drained;
    }

    /**
     * Pops the next stack if its dispatch time has come, at most one per call so a lag
     * spike spreads the trail out again instead of releasing a clump of parcels at once.
     *
     * @return the stack to ship now, or {@code null} if it is not time yet (or empty)
     */
    public ItemStack pollReady() {
        if (pending.isEmpty() || System.currentTimeMillis() < nextDispatchTime) return null;
        nextDispatchTime = System.currentTimeMillis() + DISPATCH_INTERVAL_MS;
        return pending.pollFirst();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("start", NbtUtils.writeBlockPos(start));
        tag.put("end", NbtUtils.writeBlockPos(end));
        tag.putLong("dispatchDelay", Math.max(0, nextDispatchTime - System.currentTimeMillis()));

        ListTag stacksTag = new ListTag();
        for (ItemStack stack : pending) {
            stacksTag.add(stack.save(new CompoundTag()));
        }
        tag.put("pending", stacksTag);
        return tag;
    }

    public static ParcelTrail fromCompoundTag(CompoundTag tag) {
        ParcelTrail trail = new ParcelTrail();
        trail.start = NbtUtils.readBlockPos(tag.getCompound("start"));
        trail.end = NbtUtils.readBlockPos(tag.getCompound("end"));
        trail.nextDispatchTime = System.currentTimeMillis() + tag.getLong("dispatchDelay");

        ListTag stacksTag = tag.getList("pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < stacksTag.size(); i++) {
            ItemStack stack = ItemStack.of(stacksTag.getCompound(i));
            if (!stack.isEmpty()) trail.pending.addLast(stack);
        }
        return trail;
    }
}

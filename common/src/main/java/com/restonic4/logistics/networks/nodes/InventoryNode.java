package com.restonic4.logistics.networks.nodes;

import com.restonic4.logistics.networks.flags.NetworkFlag;
import com.restonic4.logistics.networks.pathfinding.Parcel;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public abstract class InventoryNode extends ItemNode {
    private final Deque<InventoryDelta> pendingDeltas = new ArrayDeque<>();
    @Nullable private List<ItemStack> lastKnownSnapshot = null;

    public InventoryNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Nullable protected abstract BlockPos resolveTargetPos();

    public List<ItemStack> readInventory(ServerLevel level) {
        Container container = resolveContainer(level);

        if (container != null) {
            flushDeltasToContainer(container);
            lastKnownSnapshot = readFromContainer(container);
            return new ArrayList<>(lastKnownSnapshot);
        }

        if (lastKnownSnapshot != null) {
            return new ArrayList<>(lastKnownSnapshot);
        }

        return new ArrayList<>();
    }

    public void writeInventory(ServerLevel level, int index, ItemStack stack) {
        Container container = resolveContainer(level);

        if (container != null) {
            flushDeltasToContainer(container);
            container.setItem(index, stack.copy());
            if (lastKnownSnapshot != null && index < lastKnownSnapshot.size()) {
                lastKnownSnapshot.set(index, stack.copy());
            }
        } else {
            InventoryDelta delta = new InventoryDelta(index, stack);
            pendingDeltas.addLast(delta);
            applyDeltaToSnapshot(delta);
            markDirty(NetworkFlag.HAS_DELTA_CHANGES);
        }
    }

    public void onTargetChunkUnloading(ServerLevel level) {
        Container container = resolveContainer(level);
        if (container == null) return;

        lastKnownSnapshot = readFromContainer(container);
    }

    public void onTargetChunkLoaded(ServerLevel level) {
        if (pendingDeltas.isEmpty()) return;

        Container container = resolveContainer(level);
        if (container == null) return;

        flushDeltasToContainer(container);
        lastKnownSnapshot = readFromContainer(container);
    }

    public void dumpParcelOnContainer(Parcel parcel) {
        if (parcel.isEmpty()) return;

        ServerLevel level = getNetwork().getServerLevel();
        List<ItemStack> inventory = this.readInventory(level);

        // Try to merge with existing stacks first
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack existing = inventory.get(i);

            if (ItemStack.isSameItemSameTags(existing, parcel.getItemStackClone())) {
                int canAdd = Math.min(parcel.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (canAdd > 0) {
                    existing.grow(canAdd);
                    parcel.shrink(canAdd);
                    this.writeInventory(level, i, existing);
                }
            }
            if (parcel.isEmpty()) break;
        }

        // If still has items, find empty slots
        if (!parcel.isEmpty()) {
            for (int i = 0; i < inventory.size(); i++) {
                if (parcel.isEmpty()) break;

                if (inventory.get(i).isEmpty()) {
                    int amountToPut = Math.min(parcel.getCount(), parcel.getMaxStackSize());

                    ItemStack toInsert = parcel.getItemStackClone();
                    toInsert.setCount(amountToPut);

                    this.writeInventory(level, i, toInsert);

                    parcel.shrink(amountToPut);
                }
            }
        }

        // If chest is full, drop it on the floor
        if (!parcel.isEmpty()) {
            BlockPos dropPos = getBlockPos();
            ItemEntity entity = new ItemEntity(level, dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5, parcel.getItemStackClone());
            level.addFreshEntity(entity);
        }
    }

    public boolean consumeItem(ItemStack toConsume, ServerLevel level) {
        if (toConsume.getCount() > 64) {
            return false;
        }

        List<ItemStack> inventory = this.readInventory(level);

        int totalAvailable = 0;
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, toConsume)) {
                totalAvailable += stack.getCount();
            }
        }

        if (totalAvailable < toConsume.getCount()) {
            return false;
        }

        // Consume the items since we confirmed we have enough
        int remainingToConsume = toConsume.getCount();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stackInSlot = inventory.get(i);

            if (!stackInSlot.isEmpty() && ItemStack.isSameItemSameTags(stackInSlot, toConsume)) {
                int canTake = Math.min(remainingToConsume, stackInSlot.getCount());

                stackInSlot.shrink(canTake);
                remainingToConsume -= canTake;

                this.writeInventory(level, i, stackInSlot);

                if (remainingToConsume <= 0) {
                    break;
                }
            }
        }

        return true;
    }

    public boolean hasPendingDeltas() {
        return !pendingDeltas.isEmpty();
    }

    public int getTotalPendingDeltas() {
        return pendingDeltas.size();
    }

    @Nullable
    private Container resolveContainer(ServerLevel level) {
        BlockPos target = resolveTargetPos();
        if (target == null) return null;
        if (!level.isLoaded(target)) return null;
        BlockEntity be = level.getBlockEntity(target);
        if (be instanceof Container container) return container;
        return null;
    }

    private void flushDeltasToContainer(Container container) {
        while (!pendingDeltas.isEmpty()) {
            InventoryDelta delta = pendingDeltas.pollFirst();
            if (delta.index() < container.getContainerSize()) {
                container.setItem(delta.index(), delta.stack());
            }
        }
    }

    private void applyDeltaToSnapshot(InventoryDelta delta) {
        if (lastKnownSnapshot == null) return;
        if (delta.index() < lastKnownSnapshot.size()) {
            lastKnownSnapshot.set(delta.index(), delta.stack());
        }
    }

    private List<ItemStack> readFromContainer(Container container) {
        List<ItemStack> result = new ArrayList<>(container.getContainerSize());
        for (int i = 0; i < container.getContainerSize(); i++) {
            result.add(container.getItem(i).copy());
        }
        return result;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);

        if (!pendingDeltas.isEmpty()) {
            ListTag deltasTag = new ListTag();
            for (InventoryDelta delta : pendingDeltas) {
                deltasTag.add(delta.save());
            }
            tag.put("pendingDeltas", deltasTag);
        }

        if (lastKnownSnapshot != null) {
            ListTag snapshotTag = new ListTag();
            for (int i = 0; i < lastKnownSnapshot.size(); i++) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("slot", i);
                lastKnownSnapshot.get(i).save(slotTag);
                snapshotTag.add(slotTag);
            }
            tag.put("lastKnownSnapshot", snapshotTag);
        }
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);

        pendingDeltas.clear();
        if (tag.contains("pendingDeltas", Tag.TAG_LIST)) {
            ListTag deltasTag = tag.getList("pendingDeltas", Tag.TAG_COMPOUND);
            for (int i = 0; i < deltasTag.size(); i++) {
                pendingDeltas.addLast(InventoryDelta.load(deltasTag.getCompound(i)));
            }
        }

        lastKnownSnapshot = null;
        if (tag.contains("lastKnownSnapshot", Tag.TAG_LIST)) {
            ListTag snapshotTag = tag.getList("lastKnownSnapshot", Tag.TAG_COMPOUND);
            lastKnownSnapshot = new ArrayList<>(snapshotTag.size());
            for (int i = 0; i < snapshotTag.size(); i++) {
                lastKnownSnapshot.add(ItemStack.of(snapshotTag.getCompound(i)));
            }
        }
    }
}

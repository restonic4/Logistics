package com.restonic4.logistics.networks.nodes;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.networks.flags.NetworkFlag;
import com.restonic4.logistics.networks.pathfinding.Parcel;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import com.restonic4.logistics.utils.MinecraftUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
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

    public List<ItemStack> getRawPhysicalInventory(ServerLevel level) {
        Container container = resolveContainer(level);

        if (container != null) {
            Constants.LOG.error("Snapshot at getRawPhysicalInventory");
            flushDeltasToContainer(container);
            lastKnownSnapshot = readFromContainer(container);
            return new ArrayList<>(lastKnownSnapshot);
        }

        if (lastKnownSnapshot != null) {
            return new ArrayList<>(lastKnownSnapshot);
        }

        return new ArrayList<>();
    }

    public void writeRawPhysicalInventory(ServerLevel level, int index, ItemStack stack) {
        Container container = resolveContainer(level);

        if (container != null) {
            flushDeltasToContainer(container);
            container.setItem(index, stack.copy());
        } else {
            InventoryDelta delta = new InventoryDelta(index, stack.copy());
            pendingDeltas.addLast(delta);
        }
    }

    public List<ItemStack> getVirtualInventory(ServerLevel level) {
        List<ItemStack> rawInv = getRawPhysicalInventory(level);

        List<ItemStack> workingInv = new ArrayList<>(rawInv.size());
        for (ItemStack stack : rawInv) {
            workingInv.add(stack.copy());
        }

        for (InventoryDelta delta : pendingDeltas) {
            if (delta.index() >= 0 && delta.index() < workingInv.size()) {
                workingInv.set(delta.index(), delta.stack().copy());
            }
        }

        return workingInv;
    }

    public void onTargetChunkLoaded(ServerLevel level, LevelChunk levelChunk) {
        Constants.LOG.info("Loading accessor ({})!", this.getBlockPos());

        if (pendingDeltas.isEmpty()) {
            Constants.LOG.info("Empty deltas at chunk load");
            return;
        }

        Constants.LOG.info("Found deltas at chunk load");

        Container container = resolveContainer(levelChunk);
        if (container == null) {
            Constants.LOG.info("Could not resolve container at chunk load");
            return;
        }

        Constants.LOG.info("Found container at chunk load");

        detectExternalModifications(level, container);
        flushDeltasToContainer(container);
        lastKnownSnapshot = readFromContainer(container);
    }

    public void onTargetChunkUnloading(ServerLevel level, LevelChunk levelChunk) {
        Constants.LOG.info("Unloading accessor ({})!", this.getBlockPos());

        Container container = resolveContainer(levelChunk);
        if (container == null) {
            Constants.LOG.info("Could not resolve container at chunk unload");
            return;
        }

        Constants.LOG.error("Snapshot at onTargetChunkUnloading");
        lastKnownSnapshot = readFromContainer(container);
    }

    private void detectExternalModifications(ServerLevel serverLevel, Container container) {
        boolean anyDivergence = false;
        if (lastKnownSnapshot == null) return;

        int checkSize = Math.min(lastKnownSnapshot.size(), container.getContainerSize());

        for (int i = 0; i < checkSize; i++) {
            ItemStack snapshotStack = lastKnownSnapshot.get(i);
            ItemStack liveStack = container.getItem(i);

            boolean sameItem = ItemStack.isSameItemSameTags(snapshotStack, liveStack);
            boolean sameCount = snapshotStack.getCount() == liveStack.getCount();

            if (!sameItem || !sameCount) {
                if (!anyDivergence) {
                    String message = String.format(
                            "Container at %s was modified externally while its chunk was unloaded (node: %s)! " +
                            "This may indicate another mod is also writing deltas to this container. " +
                            "Our pending deltas will still be applied and may overwrite these external changes.",
                        resolveTargetPos(), this.getBlockPos()
                    );
                    Constants.LOG.warn(message);
                    Component chatMessage = Component.literal("[Logistics]: " + message).withStyle(ChatFormatting.YELLOW);
                    for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                        if (player.hasPermissions(2)) {
                            player.sendSystemMessage(chatMessage);
                        }
                    }

                    anyDivergence = true;
                }

                String message = String.format(
                        "  Slot %s: expected [%sx%s], found [%sx%s]",
                        i,
                        snapshotStack.isEmpty() ? "empty" : snapshotStack.getItem().getDescriptionId(), snapshotStack.getCount(),
                        liveStack.isEmpty() ? "empty" : liveStack.getItem().getDescriptionId(), liveStack.getCount()
                );
                Constants.LOG.warn(message);
                Component chatMessage = Component.literal("[Logistics]: " + message).withStyle(ChatFormatting.YELLOW);
                for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                    if (player.hasPermissions(2)) {
                        player.sendSystemMessage(chatMessage);
                    }
                }
            }
        }
    }

    public void dumpParcelOnContainer(Parcel parcel) {
        if (parcel.isEmpty()) return;

        ServerLevel level = getNetwork().getServerLevel();
        List<ItemStack> inventory = this.getVirtualInventory(level);

        // Try to merge with existing stacks first
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack virtualStack = inventory.get(i);

            if (ItemStack.isSameItemSameTags(virtualStack, parcel.getItemStackClone())) {
                int canAdd = Math.min(parcel.getCount(), virtualStack.getMaxStackSize() - virtualStack.getCount());
                if (canAdd > 0) {
                    virtualStack = virtualStack.copy();
                    virtualStack.grow(canAdd);
                    parcel.shrink(canAdd);
                    this.writeRawPhysicalInventory(level, i, virtualStack);
                    inventory.set(i, virtualStack);
                }
            }
            if (parcel.isEmpty()) break;
        }

        // If still has items, find empty slots
        if (!parcel.isEmpty()) {
            for (int i = 0; i < inventory.size(); i++) {
                if (inventory.get(i).isEmpty()) {
                    int amountToPut = Math.min(parcel.getCount(), parcel.getMaxStackSize());

                    ItemStack toInsert = parcel.getItemStackClone();
                    toInsert.setCount(amountToPut);

                    this.writeRawPhysicalInventory(level, i, toInsert);
                    inventory.set(i, toInsert);

                    parcel.shrink(amountToPut);
                }
                if (parcel.isEmpty()) break;
            }
        }

        // If container is full, drop it on the floor
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

        List<ItemStack> inventory = this.getVirtualInventory(level);

        int totalAvailable = 0;
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, toConsume)) {
                totalAvailable += stack.getCount();
            }
        }

        if (totalAvailable < toConsume.getCount()) return false;

        // Consume the items since we confirmed we have enough
        int remainingToConsume = toConsume.getCount();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stackInSlot = inventory.get(i);

            if (!stackInSlot.isEmpty() && ItemStack.isSameItemSameTags(stackInSlot, toConsume)) {
                int canTake = Math.min(remainingToConsume, stackInSlot.getCount());

                stackInSlot = stackInSlot.copy();
                stackInSlot.shrink(canTake);
                remainingToConsume -= canTake;

                this.writeRawPhysicalInventory(level, i, stackInSlot);
                inventory.set(i, stackInSlot);

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

    @Nullable
    private Container resolveContainer(LevelChunk levelChunk) {
        BlockPos target = resolveTargetPos();
        if (target == null) return null;
        BlockEntity be = levelChunk.getBlockEntity(target);
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

package com.restonic4.logistics.blocks.accersor;

import com.restonic4.logistics.networks.nodes.FacingNode;
import com.restonic4.logistics.networks.nodes.InventoryNode;
import com.restonic4.logistics.networks.pathfinding.Parcel;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class AccessorNode extends InventoryNode implements FacingNode {
    @Nullable private Direction facing = null;

    public AccessorNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void onParcelArrived(Parcel parcel) {
        this.dumpParcelOnContainer(parcel);
    }

    @Override
    public void setFacing(@NotNull Direction facing) {
        this.onFacingChange(this.facing, facing, this);
        this.facing = facing;
    }

    @Override
    @Nullable public Direction getFacing() {
        return facing;
    }

    @Override
    @Nullable protected BlockPos resolveTargetPos() {
        if (facing == null) return null;
        return getBlockPos().relative(facing);
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        this.saveFacing(tag);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        this.loadFacing(tag);
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        boolean added = super.buildDebugScannerTooltip(builder, isSneaking);

        builder.spacer();
        builder.keyValue("Facing", facing != null ? facing.getName() : "unset", ChatFormatting.YELLOW);
        builder.keyValue("Pending deltas", String.valueOf(getTotalPendingDeltas()), ChatFormatting.YELLOW);
        builder.keyValue("Target pos", String.valueOf(resolveTargetPos()), ChatFormatting.YELLOW);

        builder.spacer();

        ServerLevel level = getNetwork().getServerLevel();
        List<ItemStack> inv = getVirtualInventory(level);

        List<Integer> airs = new ArrayList<>();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack itemStack = inv.get(i);

            if (itemStack.isEmpty()) {
                airs.add(i);
            } else {
                String resourceLocation = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
                builder.keyValue(String.valueOf(i), resourceLocation, ChatFormatting.GOLD);
            }
        }

        String airIndicesString = airs.stream().map(String::valueOf).collect(Collectors.joining(", "));
        builder.keyValue("Air Slots", airIndicesString.isEmpty() ? "None" : airIndicesString, ChatFormatting.GOLD);

        return added;
    }
}

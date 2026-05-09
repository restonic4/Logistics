package com.restonic4.logistics.blocks.accersor;

import com.restonic4.logistics.networks.nodes.InventoryNode;
import com.restonic4.logistics.networks.pathfinding.Parcel;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AccessorNode extends InventoryNode {
    @Nullable private Direction facing = null;

    public AccessorNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void onParcelArrived(Parcel parcel) {
        this.dumpParcelOnContainer(parcel);
    }

    public void setFacing(@NotNull Direction facing) {
        this.facing = facing;
    }

    @Nullable
    public Direction getFacing() {
        return facing;
    }

    @Override
    @Nullable protected BlockPos resolveTargetPos() {
        if (facing == null) return null;
        return getBlockPos().relative(facing.getOpposite());
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        if (facing != null) {
            tag.putString("facing", facing.getSerializedName());
        }
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        if (tag.contains("facing")) {
            facing = Direction.byName(tag.getString("facing"));
        }
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        boolean added = super.buildDebugScannerTooltip(builder, isSneaking);

        builder.spacer();
        builder.keyValue("Facing", facing != null ? facing.getName() : "unset", ChatFormatting.YELLOW);
        builder.keyValue("Pending deltas", String.valueOf(getTotalPendingDeltas()), ChatFormatting.YELLOW);
        builder.keyValue("Target pos", String.valueOf(resolveTargetPos()), ChatFormatting.YELLOW);

        return added;
    }
}

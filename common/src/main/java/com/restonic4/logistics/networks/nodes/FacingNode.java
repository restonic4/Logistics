package com.restonic4.logistics.networks.nodes;

import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FacingNode {
    void setFacing(@NotNull Direction facing);
    @Nullable Direction getFacing();

    default void onFacingChange(@Nullable Direction oldFacing, Direction newFacing, @Nullable NetworkNode node) {
        if (oldFacing == null) return; // Probably just added or loaded
        if (node == null) return;
        if (node.getNetwork() == null) return;

        if (oldFacing != newFacing) {
            ServerLevel serverLevel = node.getNetwork().getServerLevel();
            BlockPos pos = node.getBlockPos();

            serverLevel.updateNeighborsAt(pos, serverLevel.getBlockState(pos).getBlock());

            NetworkManager.get(serverLevel).onMemberRemoved(pos);
            NetworkManager.get(serverLevel).onMemberPlaced(node);
        }
    }

    default void saveFacing(CompoundTag tag) {
        Direction facing = getFacing();
        if (facing != null) {
            tag.putString("facing", facing.getSerializedName());
        }
    }

    default void loadFacing(CompoundTag tag) {
        if (tag.contains("facing")) {
            Direction facing = Direction.byName(tag.getString("facing"));
            if (facing != null) {
                setFacing(facing);
            }
        }
    }

    default void writeFacing(FriendlyByteBuf buf) {
        Direction facing = getFacing();
        boolean hasFacing = facing != null;
        buf.writeBoolean(hasFacing);

        if (hasFacing) {
            buf.writeEnum(facing);
        }
    }

    default void readFacing(FriendlyByteBuf buf) {
        boolean hasFacing = buf.readBoolean();
        if (hasFacing) {
            setFacing(buf.readEnum(Direction.class));
        }
    }
}

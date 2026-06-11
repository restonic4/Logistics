package com.restonic4.logistics.blocks.base;

import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NameIdentifier {
    void setName(@NotNull String name);
    @Nullable String getName();
    default String getSafeName() {
        return getName() != null ? getName() : "Unnamed";
    }

    default void onNameChange(@Nullable String oldName, String newName, @Nullable NetworkNode node) {
        if (node == null) return;
        if (node.getNetwork() == null) return;

        if (oldName == null || !oldName.equals(newName)) {
            node.setNetworkDirty();
        }
    }

    default boolean buildNameScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        if (getName() != null) {
            builder.keyValue("Name", getName(), ChatFormatting.GOLD);
            return true;
        }

        return false;
    }

    default void saveName(CompoundTag tag) {
        String name = getName();
        if (name != null) {
            tag.putString("name", name);
        }
    }

    default void loadName(CompoundTag tag) {
        if (tag.contains("name")) {
            String name = tag.getString("name");
            setName(name);
        }
    }

    default void writeName(FriendlyByteBuf buf) {
        String name = getName();
        boolean hasName = name != null;
        buf.writeBoolean(hasName);

        if (hasName) {
            buf.writeUtf(name);
        }
    }

    default void readName(FriendlyByteBuf buf) {
        boolean hasName = buf.readBoolean();
        if (hasName) {
            setName(buf.readUtf());
        }
    }
}

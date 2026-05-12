package com.restonic4.logistics.blocks.network_connector;

import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.nodes.FacingNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.networks.types.ItemNetwork;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class NetworkConnectorNode extends EnergyNode implements FacingNode {
    private Direction facing;

    public NetworkConnectorNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    public @Nullable Network getFacingNetwork() {
        return getFacingNetwork(facing);
    }

    @Override
    public void setFacing(@NotNull Direction facing) {
        this.facing = facing;
    }

    @Override
    public @Nullable Direction getFacing() {
        return facing;
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
        super.buildDebugScannerTooltip(builder, isSneaking);

        builder.keyValue("Facing", facing.getSerializedName(), ChatFormatting.YELLOW);
        Network network = getFacingNetwork();
        String text;
        if (network == null) {
            text = "None";
        } else {
            text = network.getUUID().toString();
        }
        builder.keyValue("Facing Network", text, ChatFormatting.YELLOW);


        return true;
    }
}

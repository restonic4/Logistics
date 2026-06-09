package com.restonic4.logistics.blocks.network_connector;

import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.nodes.FacingNode;
import com.restonic4.logistics.networks.nodes.ItemNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.networks.types.ItemNetwork;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class NetworkConnectorNode extends EnergyNode implements FacingNode {
    private Direction facing;

    public NetworkConnectorNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    public @Nullable Network getBridgedNetwork() {
        if (facing == null) return null;

        Network front = getFacingNetwork(facing);
        if (front != null && !(front instanceof EnergyNetwork)) {
            return front;
        }

        Network back = getFacingNetwork(facing.getOpposite());
        if (back != null && !(back instanceof EnergyNetwork)) {
            return back;
        }

        return null;
    }

    @Override
    public void setFacing(@NotNull Direction facing) {
        this.onFacingChange(this.facing, facing, this);
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
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        super.writeExtraSyncData(buf);
        this.writeFacing(buf);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        super.readExtraSyncData(buf);
        this.readFacing(buf);
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildDebugScannerTooltip(builder, isSneaking);

        builder.keyValue("Facing", facing.getSerializedName(), ChatFormatting.YELLOW);
        Network network = getBridgedNetwork();
        String text;
        if (network == null) {
            text = "None";
        } else {
            text = network.getUUID().toString();
        }
        builder.keyValue("Bridged Network", text, ChatFormatting.YELLOW);


        return true;
    }

    // Helpers

    public boolean hasAccessors() {
        Network bridgedNetwork = getBridgedNetwork();
        if (bridgedNetwork == null) return false;
        if (!(bridgedNetwork instanceof ItemNetwork itemNetwork)) return false;
        return itemNetwork.hasAccessors();
    }

    public @Nullable List<AccessorNode> getAccessors() {
        Network bridgedNetwork = getBridgedNetwork();
        if (bridgedNetwork == null) return null;
        if (!(bridgedNetwork instanceof ItemNetwork itemNetwork)) return null;
        return itemNetwork.getAccessors();
    }
}

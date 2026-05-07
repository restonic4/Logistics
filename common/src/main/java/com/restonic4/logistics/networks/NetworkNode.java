package com.restonic4.logistics.networks;

import com.restonic4.logistics.networks.flags.DirtyFlaggable;
import com.restonic4.logistics.networks.tooltip.ScannerTooltipProvider;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public abstract class NetworkNode implements ScannerTooltipProvider {
    private final NodeTypeRegistry.NetworkNodeType<?> type;
    private UUID uuid;
    private Network network;
    private BlockPos blockPos;

    public NetworkNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        this.type = type;
        this.uuid = UUID.randomUUID();
        this.blockPos = blockPos;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public NodeTypeRegistry.NetworkNodeType<?> getType() { return type; }
    public ResourceLocation getResourceLocation() { return NodeTypeRegistry.get(this.type); }
    public UUID getUUID() { return uuid; }
    public Network getNetwork() { return network; }
    public BlockPos getBlockPos() { return blockPos; }

    public void tick() { }

    public final CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", getResourceLocation().toString());
        tag.putUUID("uuid", uuid);
        tag.putLong("pos", blockPos.asLong());
        saveExtra(tag);
        return tag;
    }

    protected void saveExtra(CompoundTag tag) {}

    public final void load(CompoundTag tag) {
        this.uuid = tag.getUUID("uuid");
        this.blockPos = BlockPos.of(tag.getLong("pos"));
        loadExtra(tag);
    }

    protected void loadExtra(CompoundTag tag) {}

    public static NetworkNode createFromTag(CompoundTag tag) {
        ResourceLocation typeId = new ResourceLocation(tag.getString("type"));
        NodeTypeRegistry.NetworkNodeType<?> type = NodeTypeRegistry.get(typeId);

        if (type == null) {
            throw new IllegalArgumentException("Unknown node type: " + typeId);
        }

        NetworkNode node = type.create(BlockPos.ZERO);
        node.load(tag);
        return node;
    }

    @Override
    public boolean buildScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        return false;
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        Network network = getNetwork();
        if (network == null) return false;

        builder.spacer();
        builder.keyValue("Node UUID", getUUID().toString(), ChatFormatting.YELLOW);
        builder.keyValue("Node type", getResourceLocation().toString(), ChatFormatting.YELLOW);

        return true;
    }

    public void markDirty(DirtyFlaggable.DirtyFlag flag) {
        Network network = getNetwork();
        if (network != null) {
            network.onNodeDirty(this, flag);
        }
    }
}

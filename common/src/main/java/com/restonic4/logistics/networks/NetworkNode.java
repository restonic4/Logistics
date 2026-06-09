package com.restonic4.logistics.networks;

import com.restonic4.logistics.networks.client.ClientNetworkManager;
import com.restonic4.logistics.networks.flags.DirtyFlaggable;
import com.restonic4.logistics.networks.flags.NetworkFlag;
import com.restonic4.logistics.networks.tooltip.ScannerTooltipProvider;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.UUID;

public abstract class NetworkNode implements ScannerTooltipProvider {
    private final NodeTypeRegistry.NetworkNodeType<?> type;
    private UUID uuid;
    private Network network;
    private BlockPos blockPos;
    private boolean isNetworkDirty = false;

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
    public ServerLevel getLevel() { return network.getServerLevel(); }
    public BlockPos getBlockPos() { return blockPos; }
    public boolean isClientSide() { return getNetwork().isClientSide(); }

    public void tick() { }
    public void onInit() { }
    public void onRemove() { }

    public <T extends Network> Optional<T> getAdjacentNetwork(Class<T> networkClass) {
        Network ownNetwork = getNetwork();
        if (ownNetwork == null) return Optional.empty();

        if (!isClientSide()) {
            ServerLevel level = ownNetwork.getServerLevel();
            return NetworkManager.get(level).getAdjacentNetwork(blockPos, networkClass);
        } else {
            return ClientNetworkManager.getAdjacentNetwork(getNetwork().getDimensionKey(), blockPos, networkClass);
        }
    }

    public Network getFacingNetwork(Direction direction) {
        Network ownNetwork = getNetwork();
        if (ownNetwork == null) return null;

        if (!isClientSide()) {
            ServerLevel level = ownNetwork.getServerLevel();
            return NetworkManager.get(level).getNetworkByBlockPos(blockPos.relative(direction));
        } else {
            return ClientNetworkManager.getNetwork(getNetwork().getDimensionKey(), blockPos.relative(direction));
        }
    }

    public <T extends Network> Optional<T> getFacingNetwork(Class<T> networkClass, Direction direction) {
        Network ownNetwork = getNetwork();
        if (ownNetwork == null) return Optional.empty();

        if (!isClientSide()) {
            ServerLevel level = ownNetwork.getServerLevel();
            return NetworkManager.get(level).getNetworkByBlockPos(networkClass, blockPos.relative(direction));
        } else {
            return (Optional<T>) Optional.of(ClientNetworkManager.getNetwork(getNetwork().getDimensionKey(), blockPos.relative(direction)));
        }
    }

    // Serialization

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

    // Packets

    public final void writeSyncData(FriendlyByteBuf buf) {
        buf.writeUUID(this.uuid);
        buf.writeBlockPos(this.blockPos);
        writeExtraSyncData(buf);
    }

    protected void writeExtraSyncData(FriendlyByteBuf buf) {}

    public final void readSyncData(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.blockPos = buf.readBlockPos();
        readExtraSyncData(buf);
    }

    protected void readExtraSyncData(FriendlyByteBuf buf) {}

    // Other

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
        builder.keyValue("Network dirty", String.valueOf(isNetworkDirty()), isNetworkDirty() ? ChatFormatting.RED : ChatFormatting.GREEN);

        return true;
    }

    public void markDirty(DirtyFlaggable.DirtyFlag flag) {
        Network network = getNetwork();
        if (network != null) {
            network.onNodeDirty(this, flag);
        }

        if (flag == NetworkFlag.NETWORK_DIRTY) {
            isNetworkDirty = true;
        }
    }

    public void setNetworkDirty() {
        markDirty(NetworkFlag.NETWORK_DIRTY);
    }

    public void cleanNetworkDirty() {
        isNetworkDirty = false;
    }

    public boolean isNetworkDirty() {
        return isNetworkDirty;
    }
}

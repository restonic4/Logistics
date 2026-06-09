package com.restonic4.logistics.networks;

import com.restonic4.logistics.networks.flags.DirtyFlaggable;
import com.restonic4.logistics.networks.flags.NetworkFlag;
import com.restonic4.logistics.networks.tooltip.ScannerTooltipProvider;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.registry.NetworkTypeRegistry;
import com.restonic4.logistics.utils.MathHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class Network implements DirtyFlaggable, ScannerTooltipProvider {
    private final NetworkTypeRegistry.NetworkType<?> type;
    private UUID uuid;
    private final NodeIndex nodeIndex;

    private final ServerLevel serverLevel;
    private ResourceKey<Level> clientDimension;

    private boolean isDirty = false;
    private long dirtyBits = 0L;

    private long lastNBTDiskSize = -1;

    private final Queue<Runnable> scheduledTasks = new ConcurrentLinkedQueue<>();

    protected Network(NetworkTypeRegistry.NetworkType<?> type, ServerLevel serverLevel) {
        this.type = type;
        this.uuid = UUID.randomUUID();
        this.serverLevel = serverLevel;
        this.nodeIndex = new NodeIndex(this);
    }

    public void execute(Runnable task) {
        this.scheduledTasks.add(task);
    }

    public void tick() {
        while (!scheduledTasks.isEmpty()) {
            Runnable task = scheduledTasks.poll();
            if (task != null) {
                task.run();
            }
        }
        nodeIndex.getAllNodes().forEach(NetworkNode::tick);
    }

    /**
     * Called when 'other' is being merged into 'this'.
     * Use this to transfer energy buffers, fluid contents, etc.
     */
    public abstract void mergeDataFrom(Network other);

    /**
     * Called when this network is being dissolved into multiple smaller networks.
     * Use this to distribute energy, fluids, or items among the children.
     * @param children The new networks created from the split.
     */
    public abstract void onSplit(Collection<Network> children);

    public NetworkTypeRegistry.NetworkType<?> getType() { return type; }
    public ResourceLocation getResourceLocation() { return NetworkTypeRegistry.get(type); }
    public UUID getUUID() { return uuid; }
    public ServerLevel getServerLevel() { return serverLevel; }
    public NodeIndex getNodeIndex() {
        return nodeIndex;
    }

    public void setDirty() { this.isDirty = true; }
    public void cleanDirtyFlag() { this.isDirty = false; }
    public boolean isDirty() { return this.isDirty; }
    public void setNetworkDirty() { markDirty(NetworkFlag.NETWORK_DIRTY); }
    public boolean isNetworkDirty() { return isDirty(NetworkFlag.NETWORK_DIRTY); }
    public void cleanNetworkDirty() { clearFlag(NetworkFlag.NETWORK_DIRTY); }
    public boolean isClientSide() { return this.serverLevel == null; }

    public ResourceKey<Level> getDimensionKey() {
        if (this.serverLevel != null) {
            return this.serverLevel.dimension();
        }
        return this.clientDimension;
    }

    // Serialization

    public final CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putString("type", getResourceLocation().toString());
        tag.putUUID("uuid", uuid);
        ListTag nodes = new ListTag();
        for (NetworkNode node : nodeIndex.getAllNodes()) {
            nodes.add(node.save());
        }
        tag.put("nodes", nodes);

        saveExtra(tag);
        return tag;
    }

    protected void saveExtra(CompoundTag tag) {}

    public final void load(CompoundTag tag) {
        this.uuid = tag.getUUID("uuid");

        ListTag nodesList = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < nodesList.size(); i++) {
            this.nodeIndex.registerFromCompoundTag(nodesList.getCompound(i));
        }

        loadExtra(tag);

        lastNBTDiskSize = estimateDiskSize();
    }

    protected void loadExtra(CompoundTag tag) {}

    // Packets

    public static Network createEmptyOnClient(UUID uuid, ResourceLocation typeId, ResourceKey<Level> dimension) {
        NetworkTypeRegistry.NetworkType<?> type = NetworkTypeRegistry.get(typeId);
        if (type == null) throw new IllegalStateException("Unknown network type on client: " + typeId);

        Network network = type.create(null);
        network.uuid = uuid;
        network.clientDimension = dimension;
        return network;
    }

    // Other

    public void onNodeDirty(NetworkNode node, DirtyFlaggable.DirtyFlag flag) {
        markDirty(flag);
        setNetworkDirty();
        lastNBTDiskSize = estimateDiskSize();
    }

    public static Network create(NetworkTypeRegistry.NetworkType<?> networkType, ServerLevel serverLevel) {
        return networkType.create(serverLevel);
    }

    public static Network createFromTag(CompoundTag tag, ServerLevel serverLevel) {
        ResourceLocation typeId = new ResourceLocation(tag.getString("type"));
        NetworkTypeRegistry.NetworkType<?> type = NetworkTypeRegistry.get(typeId);

        if (type == null) {
            throw new IllegalArgumentException("Unknown network type: " + typeId);
        }

        Network network = type.create(serverLevel);
        network.load(tag);
        return network;
    }

    @Override public long getDirtyBits() { return dirtyBits; }
    @Override public void setDirtyBits(long bits) { this.dirtyBits = bits; }

    @Override
    public boolean buildScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        return false;
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        builder.title("Debug", ChatFormatting.GOLD);
        builder.line();

        builder.keyValue("Network UUID", getUUID().toString(), ChatFormatting.YELLOW);
        builder.keyValue("Network type", getResourceLocation().toString(), ChatFormatting.YELLOW);
        builder.keyValue("Nodes", String.valueOf(getNodeIndex().size()), ChatFormatting.YELLOW);

        builder.spacer();
        builder.text("Flags (" + toBinaryString(NetworkFlag.class) + "):", ChatFormatting.YELLOW);

        List<NetworkFlag> flags = getActiveFlags(NetworkFlag.class);
        flags.forEach(f -> builder.bullet(f.name(), ChatFormatting.YELLOW));

        builder.keyValue("Disk size", MathHelper.formatBytes(lastNBTDiskSize), ChatFormatting.YELLOW);

        return true;
    }

    // Stats

    public long estimateDiskSize() {
        try {
            CompoundTag tag = this.save();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            NbtIo.write(tag, dos);

            return baos.size();
        } catch (IOException ignored) {
            return -1;
        }
    }

    /*public long estimatePacketSize() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            this.writeSyncData(buffer);
            return buffer.readableBytes();
        } catch (Exception e) {
            return -1;
        } finally {
            buffer.release();
        }
    }*/
}

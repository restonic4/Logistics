package com.restonic4.logistics.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public abstract class NetworkNode {
    private final NodeTypeRegistry.NetworkNodeType<?> type;
    private UUID uuid;
    private Network network;
    private BlockPos blockPos;

    public NetworkNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        this.type = type;
        this.uuid = UUID.randomUUID();
        this.blockPos = blockPos;
    }

    public void tick() { };

    public void setNetwork(Network network) {
        this.network = network;
    }

    public ResourceLocation getResourceLocation() { return NodeTypeRegistry.get(this.type); }
    public UUID getUUID() { return uuid; }
    public Network getNetwork() { return network; }
    public BlockPos getBlockPos() { return blockPos; }

    /*
        ENERGY MANAGEMENT
     */

    /**
     * @param amount The energy offered to this node.
     * @param simulate If true, don't actually change the energy level.
     * @return The amount of energy that was NOT accepted (overflow).
     */
    public long receiveEnergy(long amount, boolean simulate) {
        return amount; // Default: doesn't accept energy
    }

    /**
     * @param amount The energy requested from this node.
     * @param simulate If true, don't actually change the energy level.
     * @return The amount of energy actually extracted.
     */
    public long extractEnergy(long amount, boolean simulate) {
        return 0; // Default: doesn't provide energy
    }

    public long getStoredEnergy() {
        return 0;
    }

    public long getMaxStorage() {
        return 0;
    }

    /*
        DATA MANAGEMENT
     */

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
}

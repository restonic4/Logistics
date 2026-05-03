package com.restonic4.logistics.registry;

import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public final class LogisticsRegistryEntry<B extends Block, N extends NetworkNode> {
    private final ResourceLocation id;

    @Nullable private final B block;
    @Nullable private final Item item;
    @Nullable private final BlockEntityType<? extends BlockEntity> blockEntityType;
    @Nullable private final NodeTypeRegistry.NetworkNodeType<N> nodeType;

    LogisticsRegistryEntry(
            ResourceLocation id,
            @Nullable B block,
            @Nullable Item item,
            @Nullable BlockEntityType<? extends BlockEntity> blockEntityType,
            @Nullable NodeTypeRegistry.NetworkNodeType<N> nodeType
    ) {
        this.id = id;
        this.block = block;
        this.item = item;
        this.blockEntityType = blockEntityType;
        this.nodeType = nodeType;
    }

    public ResourceLocation getId() {
        return id;
    }

    public B getBlock() {
        if (block == null) throw new IllegalStateException("Entry '" + id + "' has no block.");
        return block;
    }

    public Item getItem() {
        if (item == null) throw new IllegalStateException("Entry '" + id + "' has no item. Did you forget .withItem()?");
        return item;
    }

    @SuppressWarnings("unchecked")
    public <BE extends BlockEntity> BlockEntityType<BE> getBlockEntityType(Class<BE> type) {
        if (blockEntityType == null) throw new IllegalStateException("Entry '" + id + "' has no block entity. Did you forget .withBlockEntity()?");
        return (BlockEntityType<BE>) blockEntityType;
    }

    public NodeTypeRegistry.NetworkNodeType<N> getNodeType() {
        if (nodeType == null) throw new IllegalStateException("Entry '" + id + "' has no node type. Did you forget .network()?");
        return nodeType;
    }

    public boolean hasItem() { return item != null; }
    public boolean hasBlockEntity() { return blockEntityType != null; }

    @Nullable public B getBlockOrNull() { return block; }
    @Nullable public Item getItemOrNull() { return item; }
    @Nullable public NodeTypeRegistry.NetworkNodeType<N> getNodeTypeOrNull() { return nodeType; }
    @Nullable public BlockEntityType<? extends BlockEntity> getBlockEntityTypeOrNull() { return blockEntityType; }
}

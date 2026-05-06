package com.restonic4.logistics.registry.entries;

import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class BlockEntry<B extends Block, N extends NetworkNode> {
    private final ResourceLocation id;
    private boolean loaded = false;

    @Nullable private Supplier<B> block;
    @Nullable private Supplier<BlockEntityType<? extends BlockEntity>> blockEntityType;
    @Nullable private Supplier<Item> item;
    @Nullable private NodeTypeRegistry.NetworkNodeType<N> nodeType;

    public BlockEntry(ResourceLocation id) {
        this.id = id;
    }

    public void markLoaded(
            @Nullable Supplier<B> block,
            @Nullable Supplier<BlockEntityType<? extends BlockEntity>> blockEntityType,
            @Nullable Supplier<Item> item,
            @Nullable NodeTypeRegistry.NetworkNodeType<N> nodeType
    ) {
        this.block = block;
        this.blockEntityType = blockEntityType;
        this.item = item;
        this.nodeType = nodeType;
        this.loaded = true;
    }

    private void assertLoaded() {
        if (!loaded) throw new IllegalStateException("BlockEntry '" + id + "' accessed before platform registration completed. Are you accessing it too early?");
    }

    public ResourceLocation getId() { return id; }

    public B getBlock() {
        assertLoaded();
        if (block == null) throw new IllegalStateException("Entry '" + id + "' has no block.");
        return block.get();
    }

    public Item getItem() {
        assertLoaded();
        if (item == null) throw new IllegalStateException("Entry '" + id + "' has no item.");
        return item.get();
    }

    @SuppressWarnings("unchecked")
    public <BE extends BlockEntity> BlockEntityType<BE> getBlockEntityType(Class<BE> clazz) {
        assertLoaded();
        if (blockEntityType == null) throw new IllegalStateException("Entry '" + id + "' has no block entity.");
        return (BlockEntityType<BE>) blockEntityType.get();
    }

    public NodeTypeRegistry.NetworkNodeType<N> getNodeType() {
        assertLoaded();
        if (nodeType == null) throw new IllegalStateException("Entry '" + id + "' has no node type.");
        return nodeType;
    }

    public boolean hasItem() { assertLoaded(); return item != null; }
    public boolean hasBlockEntity() { assertLoaded(); return blockEntityType != null; }

    @Nullable public B getBlockOrNull() { assertLoaded(); return block != null ? block.get() : null; }
    @Nullable public Item getItemOrNull() { assertLoaded(); return item != null ? item.get() : null; }
    @Nullable public NodeTypeRegistry.NetworkNodeType<N> getNodeTypeOrNull() { assertLoaded(); return nodeType; }
    @Nullable public BlockEntityType<? extends BlockEntity> getBlockEntityTypeOrNull() { assertLoaded(); return blockEntityType != null ? blockEntityType.get() : null; }
}
package com.restonic4.logistics.registry.builders;

import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.platform.Services;
import com.restonic4.logistics.registry.NetworkTypeRegistry;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import com.restonic4.logistics.registry.entries.BlockEntry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class BlockBuilder<B extends Block, N extends NetworkNode> {
    private final ResourceLocation id;
    private final Supplier<B> blockFactory;

    private NetworkTypeRegistry.NetworkType<?> networkType;
    private NodeTypeRegistry.NodeFactory<N> nodeFactory;

    private Function<B, Item> itemFactory;

    private BlockEntityType.BlockEntitySupplier<? extends BlockEntity> blockEntitySupplier;

    private final List<ResourceKey<CreativeModeTab>> tabs = new ArrayList<>();

    public BlockBuilder(ResourceLocation id, Supplier<B> blockFactory) {
        this.id = id;
        this.blockFactory = blockFactory;
    }

    @SuppressWarnings("unchecked")
    public <N2 extends NetworkNode> BlockBuilder<B, N2> network(
            NetworkTypeRegistry.NetworkType<?> networkType,
            NodeTypeRegistry.NodeFactory<N2> nodeFactory
    ) {
        this.networkType = networkType;
        this.nodeFactory = (NodeTypeRegistry.NodeFactory<N>) nodeFactory;
        return (BlockBuilder<B, N2>) this;
    }

    public BlockBuilder<B, N> withItem() {
        this.itemFactory = block -> new BlockItem(block, new Item.Properties());
        return this;
    }

    public BlockBuilder<B, N> withItem(Function<B, Item> factory) {
        this.itemFactory = factory;
        return this;
    }

    public BlockBuilder<B, N> withBlockEntity(
            BlockEntityType.BlockEntitySupplier<? extends BlockEntity> supplier
    ) {
        this.blockEntitySupplier = supplier;
        return this;
    }

    public final BlockBuilder<B, N> addToTab(ResourceKey<CreativeModeTab> tabKey) {
        this.tabs.add(tabKey);
        return this;
    }

    @SuppressWarnings("unchecked")
    public BlockEntry<B, N> register() {
        NodeTypeRegistry.NetworkNodeType<N> nodeType = null;
        if (this.networkType != null && this.nodeFactory != null) {
            nodeType = new NodeTypeRegistry.NetworkNodeType<>(this.networkType, this.nodeFactory);
        }
        NodeTypeRegistry.register(id, nodeType);

        return (BlockEntry<B, N>) Services.PLATFORM_REGISTRY.fromBlockBuilder(
                this.id,
                this.blockFactory,
                (BlockEntityType.BlockEntitySupplier<BlockEntity>) this.blockEntitySupplier,
                (Function<Block, Item>) this.itemFactory,
                (NodeTypeRegistry.NetworkNodeType<NetworkNode>) nodeType,
                tabs
        );
    }
}

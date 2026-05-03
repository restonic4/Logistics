package com.restonic4.logistics.registry;

import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.registries.NetworkTypeRegistry;
import com.restonic4.logistics.networks.registries.NodeTypeRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
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

public final class BlockBuilder<B extends Block, N extends NetworkNode> {
    private final ResourceLocation id;
    private final Supplier<B> blockFactory;

    private NetworkTypeRegistry.NetworkType<?> networkType;
    private NodeTypeRegistry.NodeFactory<N> nodeFactory;

    private Function<B, Item> itemFactory;

    private BlockEntityType.BlockEntitySupplier<? extends BlockEntity> blockEntitySupplier;

    private final List<ResourceKey<CreativeModeTab>> tabs = new ArrayList<>();

    BlockBuilder(ResourceLocation id, Supplier<B> blockFactory) {
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

    @SafeVarargs
    public final BlockBuilder<B, N> addToTab(ResourceKey<CreativeModeTab>... tabKeys) {
        tabs.addAll(List.of(tabKeys));
        return this;
    }

    @SuppressWarnings("unchecked")
    public LogisticsRegistryEntry<B, N> register() {
        // Node type
        NodeTypeRegistry.NetworkNodeType<N> nodeType = null;
        if (networkType != null && nodeFactory != null) {
            nodeType = NodeTypeRegistry.register(id, networkType, nodeFactory);
        }

        // Block
        B block = blockFactory.get();
        if (block instanceof BaseNetworkBlock networkBlock && nodeType != null) {
            networkBlock.setNodeType(nodeType);
        }
        Registry.register(BuiltInRegistries.BLOCK, id, block);

        // Item
        Item item = null;
        if (itemFactory != null) {
            item = itemFactory.apply(block);
            Registry.register(BuiltInRegistries.ITEM, id, item);
        }

        // Creative tabs
        if (item != null && !tabs.isEmpty()) {
            final Item finalItem = item;
            for (ResourceKey<CreativeModeTab> tab : tabs) {
                ItemGroupEvents.modifyEntriesEvent(tab).register(entries -> entries.accept(finalItem));
            }
        }

        // Block entity
        BlockEntityType<? extends BlockEntity> blockEntityType = null;
        if (blockEntitySupplier != null) {
            blockEntityType = BlockEntityType.Builder
                    .of(blockEntitySupplier, block)
                    .build(null);
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, blockEntityType);
        }

        return new LogisticsRegistryEntry<>(id, block, item, blockEntityType, nodeType);
    }
}

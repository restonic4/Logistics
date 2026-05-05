package com.restonic4.logistics.registry;

import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.networks.NetworkNode;
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

    private final List<Supplier<ResourceKey<CreativeModeTab>>> tabs = new ArrayList<>();

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

    public final BlockBuilder<B, N> addToTab(Supplier<ResourceKey<CreativeModeTab>> tabKeySupplier) {
        this.tabs.add(tabKeySupplier);
        return this;
    }

    @SuppressWarnings("unchecked")
    public BlockEntry<B, N> register() {
        // Node type
        NodeTypeRegistry.NetworkNodeType<N> nodeType;
        if (networkType != null && nodeFactory != null) {
            nodeType = new NodeTypeRegistry.NetworkNodeType<>(networkType, nodeFactory);
        } else {
            nodeType = null;
        }

        // Block
        B block = blockFactory.get();
        if (block instanceof BaseNetworkBlock networkBlock && nodeType != null) {
            networkBlock.setNodeType(nodeType);
        }

        // Item
        Item item = (itemFactory != null) ? itemFactory.apply(block) : null;

        // Block entity
        BlockEntityType<? extends BlockEntity> blockEntityType = (blockEntitySupplier != null)
                ? BlockEntityType.Builder.of(blockEntitySupplier, block).build(null)
                : null;

        Registrate.delay(() -> {
            if (nodeType != null) {
                NodeTypeRegistry.register(id, nodeType);
            }

            Registry.register(BuiltInRegistries.BLOCK, id, block);

            if (item != null) {
                Registry.register(BuiltInRegistries.ITEM, id, item);

                for (Supplier<ResourceKey<CreativeModeTab>> tabSupplier : tabs) {
                    CreativeTabRegistry.scheduleInjection(tabSupplier.get(), () -> item);
                }
            }

            if (blockEntityType != null) {
                Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, blockEntityType);
            }
        });

        return new BlockEntry<>(id, block, item, blockEntityType, nodeType);
    }
}

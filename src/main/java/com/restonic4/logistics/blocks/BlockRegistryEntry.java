package com.restonic4.logistics.blocks;

import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.registries.NetworkTypeRegistry;
import com.restonic4.logistics.networks.registries.NodeTypeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public class BlockRegistryEntry<B extends BaseNetworkBlock, N extends EnergyNode> {
    private ResourceLocation resourceLocation;

    private final Supplier<B> blockFactory;
    private NodeTypeRegistry.NetworkNodeType<N> nodeType;

    private B block;
    private Item item;
    private BlockEntityType<?> blockEntityType;

    public BlockRegistryEntry(Supplier<B> blockFactory) {
        this.blockFactory = blockFactory;
    }

    public void register(
            ResourceLocation id,
            NetworkTypeRegistry.NetworkType<?> networkType,
            NodeTypeRegistry.NodeFactory<N> nodeFactory,
            Function<B, Item> itemFactory,
            BlockEntityType.BlockEntitySupplier<?> blockEntitySupplier
    ) {
        // ResourceLocation
        this.resourceLocation = id;

        // Node type
        this.nodeType = NodeTypeRegistry.register(id, networkType, nodeFactory);

        // Block
        this.block = blockFactory.get();
        this.block.setNodeType(this.nodeType);
        Registry.register(BuiltInRegistries.BLOCK, id, this.block);

        // Item
        this.item = itemFactory.apply(this.block);
        Registry.register(BuiltInRegistries.ITEM, id, this.item);

        // Block entity (optional)
        if (blockEntitySupplier != null) {
            this.blockEntityType = BlockEntityType.Builder
                    .of(blockEntitySupplier, this.block)
                    .build(null);
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, this.blockEntityType);
        }
    }

    public void register(
            ResourceLocation id,
            NetworkTypeRegistry.NetworkType<?> networkType,
            NodeTypeRegistry.NodeFactory<N> nodeFactory,
            Function<B, Item> itemFactory
    ) {
        register(id, networkType, nodeFactory, itemFactory, null);
    }

    public B getBlock() {
        if (block == null) block = blockFactory.get();
        return block;
    }

    public Item getItem() { return item; }
    public @Nullable BlockEntityType<? extends BlockEntity> getBlockEntityType() { return blockEntityType; }
    public NodeTypeRegistry.NetworkNodeType<? extends EnergyNode> getNodeType() { return nodeType; }
    public ResourceLocation getResourceLocation() { return resourceLocation; }
}

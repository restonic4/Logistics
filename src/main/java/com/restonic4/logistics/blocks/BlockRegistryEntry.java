package com.restonic4.logistics.blocks;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.blocks.battery.BatteryNode;
import com.restonic4.logistics.energy.NetworkNode;
import com.restonic4.logistics.energy.NodeTypeRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public class BlockRegistryEntry<B extends BaseNetworkBlock, N extends NetworkNode> {
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
            NodeTypeRegistry.NodeFactory<N> nodeFactory,
            Function<B, Item> itemFactory,
            BlockEntityType.BlockEntitySupplier<?> blockEntitySupplier
    ) {
        // ResourceLocation
        this.resourceLocation = id;

        // Node type
        this.nodeType = NodeTypeRegistry.register(id, nodeFactory);

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
            NodeTypeRegistry.NodeFactory<N> nodeFactory,
            Function<B, Item> itemFactory
    ) {
        register(id, nodeFactory, itemFactory, null);
    }

    public B getBlock() {
        if (block == null) block = blockFactory.get();
        return block;
    }

    public Item getItem() { return item; }
    public @Nullable BlockEntityType<? extends BlockEntity> getBlockEntityType() { return blockEntityType; }
    public NodeTypeRegistry.NetworkNodeType<? extends NetworkNode> getNodeType() { return nodeType; }
    public ResourceLocation getResourceLocation() { return resourceLocation; }
}

package com.restonic4.logistics.blocks;

import com.restonic4.logistics.Logistics;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public class BlockRegistryEntry {
    private final Block block;
    private final Item item;
    private final BlockEntityType<? extends BlockEntity> blockEntityType;

    private BlockRegistryEntry(Block block, Item item, BlockEntityType<? extends BlockEntity> blockEntityType) {
        this.block = block;
        this.item = item;
        this.blockEntityType = blockEntityType;
    }

    public static <T extends BlockEntity> BlockRegistryEntry register(String id, Block block, FabricBlockEntityTypeBuilder.Factory<T> blockEntityFactory) {
        Block registeredBlock = Registry.register(BuiltInRegistries.BLOCK, Logistics.id(id), block);
        Item registeredItem = Registry.register(BuiltInRegistries.ITEM, Logistics.id(id), new BlockItem(block, new FabricItemSettings()));
        BlockEntityType<T> registeredBlockEntity = (blockEntityFactory == null) ? null : Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Logistics.id(id),
                FabricBlockEntityTypeBuilder.create(blockEntityFactory, registeredBlock).build()
        );

        return new BlockRegistryEntry(registeredBlock, registeredItem, registeredBlockEntity);
    }

    public Block getBlock() {
        return block;
    }

    public Item getItem() {
        return item;
    }
    
    public @Nullable BlockEntityType<? extends BlockEntity> getBlockEntityType() {
        return blockEntityType;
    }
}

package com.restonic4.logistics.blocks;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.Logistics;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class BlockRegistry {
    public static final Block BASIC_PIPE = registerBlock("basic_pipe",
            new PipeBlock(FabricBlockSettings.copyOf(Blocks.GLASS).nonOpaque().dynamicShape().noOcclusion()));

    private static Block registerBlock(String id, Block block) {
        registerBlockItem(id, block);
        return Registry.register(BuiltInRegistries.BLOCK, Logistics.id(id), block);
    }

    private static Item registerBlockItem(String id, Block block) {
        return Registry.register(BuiltInRegistries.ITEM, Logistics.id(id),
                new BlockItem(block, new FabricItemSettings()));
    }

    public static void register() {

    }
}

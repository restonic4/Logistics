package com.restonic4.logistics.blocks;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.machine.BatteryBlock;
import com.restonic4.logistics.blocks.machine.GeneratorBlock;
import com.restonic4.logistics.blocks.machine.MachineBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class BlockRegistry {

    // --- Pipes ---
    public static final Block BASIC_PIPE = registerBlock("basic_pipe",
            new PipeBlock(FabricBlockSettings.copyOf(Blocks.GLASS).nonOpaque().dynamicShape().noOcclusion()));

    // --- Energy test blocks ---
    public static final Block GENERATOR_BLOCK = registerBlock("generator",
            new GeneratorBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).noOcclusion()));

    public static final Block MACHINE_BLOCK = registerBlock("machine",
            new MachineBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).noOcclusion()));

    public static final Block BATTERY_BLOCK = registerBlock("battery",
            new BatteryBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).noOcclusion()));

    // -------------------------------------------------------------------------

    private static Block registerBlock(String id, Block block) {
        registerBlockItem(id, block);
        return Registry.register(BuiltInRegistries.BLOCK, Logistics.id(id), block);
    }

    private static Item registerBlockItem(String id, Block block) {
        return Registry.register(BuiltInRegistries.ITEM, Logistics.id(id),
                new BlockItem(block, new FabricItemSettings()));
    }

    public static void register() {
        // Static field access triggers registration above.
    }
}
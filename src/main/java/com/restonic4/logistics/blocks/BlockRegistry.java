package com.restonic4.logistics.blocks;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.battery.BatteryBlock;
import com.restonic4.logistics.blocks.battery.BatteryBlockItem;
import com.restonic4.logistics.blocks.battery.BatteryNode;
import com.restonic4.logistics.blocks.generator.GeneratorBlock;
import com.restonic4.logistics.blocks.generator.GeneratorNode;
import com.restonic4.logistics.blocks.machine.MachineBlock;
import com.restonic4.logistics.blocks.machine.MachineNode;
import com.restonic4.logistics.blocks.pipe.PipeBlock;
import com.restonic4.logistics.blocks.pipe.PipeNode;
import com.restonic4.logistics.energy.NodeTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;

public class BlockRegistry {
    public static final BlockRegistryEntry<PipeBlock, PipeNode> BASIC_PIPE = new BlockRegistryEntry<>(
            () -> new PipeBlock(FabricBlockSettings.copyOf(Blocks.GLASS).nonOpaque().dynamicShape().noOcclusion().requiresCorrectToolForDrops())
    );

    public static final BlockRegistryEntry<GeneratorBlock, GeneratorNode> GENERATOR_BLOCK = new BlockRegistryEntry<>(
            () -> new GeneratorBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
    );

    public static final BlockRegistryEntry<MachineBlock, MachineNode> MACHINE_BLOCK = new BlockRegistryEntry<>(
            () -> new MachineBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
    );

    public static final BlockRegistryEntry<BatteryBlock, BatteryNode> BATTERY_BLOCK = new BlockRegistryEntry<>(
            () -> new BatteryBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
    );

    public static void register() {
        BASIC_PIPE.register(Logistics.id("basic_pipe"), PipeNode::new, block -> new BlockItem(block, new Item.Properties()));
        GENERATOR_BLOCK.register(Logistics.id("generator"), GeneratorNode::new, block -> new BlockItem(block, new Item.Properties()));
        MACHINE_BLOCK.register(Logistics.id("machine"), MachineNode::new, block -> new BlockItem(block, new Item.Properties()));
        BATTERY_BLOCK.register(Logistics.id("battery"), BatteryNode::new, block -> new BatteryBlockItem(block, new Item.Properties()));
    }
}

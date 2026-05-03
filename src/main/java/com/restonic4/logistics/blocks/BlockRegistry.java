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
import com.restonic4.logistics.networks.BuiltInNetworks;
import com.restonic4.logistics.registry.LogisticsRegistryEntry;
import com.restonic4.logistics.registry.Registrate;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;

public class BlockRegistry {
    public static final LogisticsRegistryEntry<PipeBlock, PipeNode> BASIC_PIPE = Registrate
            .block(
                    Logistics.id("basic_pipe"),
                    () -> new PipeBlock(FabricBlockSettings.copyOf(Blocks.GLASS).nonOpaque().dynamicShape().noOcclusion().requiresCorrectToolForDrops())
            )
            .network(BuiltInNetworks.ENERGY_NETWORK, PipeNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB_KEY)
            .register();

    public static final LogisticsRegistryEntry<GeneratorBlock, GeneratorNode> GENERATOR_BLOCK = Registrate
            .block(
                    Logistics.id("generator"),
                    () -> new GeneratorBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
            )
            .network(BuiltInNetworks.ENERGY_NETWORK, GeneratorNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB_KEY)
            .register();

    public static final LogisticsRegistryEntry<MachineBlock, MachineNode> MACHINE_BLOCK = Registrate
            .block(
                    Logistics.id("machine"),
                    () -> new MachineBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops()))
            .network(BuiltInNetworks.ENERGY_NETWORK, MachineNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB_KEY)
            .register();

    public static final LogisticsRegistryEntry<BatteryBlock, BatteryNode> BATTERY_BLOCK = Registrate
            .block(
                    Logistics.id("battery"),
                    () -> new BatteryBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops()))
            .network(BuiltInNetworks.ENERGY_NETWORK, BatteryNode::new)
            .withItem(block -> new BatteryBlockItem(block, new Item.Properties()))   // custom item
            .addToTab(Logistics.CUSTOM_TAB_KEY)
            .register();

    public static final LogisticsRegistryEntry<PipeBlock, PipeNode> BASIC_PIPE_2 = Registrate
            .block(
                    Logistics.id("basic_pipe_2"),
                    () -> new PipeBlock(FabricBlockSettings.copyOf(Blocks.GLASS).nonOpaque().dynamicShape().noOcclusion().requiresCorrectToolForDrops()))
            .network(BuiltInNetworks.ITEM_NETWORK, PipeNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB_KEY)
            .register();

    public static void register() {

    }
}

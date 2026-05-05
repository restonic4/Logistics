package com.restonic4.logistics.blocks;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.battery.BatteryBlock;
import com.restonic4.logistics.blocks.battery.BatteryBlockItem;
import com.restonic4.logistics.blocks.battery.BatteryNode;
import com.restonic4.logistics.blocks.generator.GeneratorBlock;
import com.restonic4.logistics.blocks.generator.GeneratorNode;
import com.restonic4.logistics.blocks.machine.MachineBlock;
import com.restonic4.logistics.blocks.machine.MachineNode;
import com.restonic4.logistics.blocks.cable.CableBlock;
import com.restonic4.logistics.blocks.cable.CableNode;
import com.restonic4.logistics.blocks.pipe.PipeBlock;
import com.restonic4.logistics.blocks.pipe.PipeNode;
import com.restonic4.logistics.networks.BuiltInNetworks;
import com.restonic4.logistics.registry.BlockEntry;
import com.restonic4.logistics.registry.Registrate;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockRegistry {
    public static final BlockEntry<CableBlock, CableNode> CABLE_BLOCK = Registrate
            .block(
                    Logistics.id("cable"),
                    () -> new CableBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).noOcclusion().dynamicShape().noOcclusion().requiresCorrectToolForDrops())
            )
            .network(BuiltInNetworks.ENERGY_NETWORK, CableNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.key())
            .register();

    public static final BlockEntry<GeneratorBlock, GeneratorNode> GENERATOR_BLOCK = Registrate
            .block(
                    Logistics.id("generator"),
                    () -> new GeneratorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
            )
            .network(BuiltInNetworks.ENERGY_NETWORK, GeneratorNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.key())
            .register();

    public static final BlockEntry<MachineBlock, MachineNode> MACHINE_BLOCK = Registrate
            .block(
                    Logistics.id("machine"),
                    () -> new MachineBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops()))
            .network(BuiltInNetworks.ENERGY_NETWORK, MachineNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.key())
            .register();

    public static final BlockEntry<BatteryBlock, BatteryNode> BATTERY_BLOCK = Registrate
            .block(
                    Logistics.id("battery"),
                    () -> new BatteryBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops()))
            .network(BuiltInNetworks.ENERGY_NETWORK, BatteryNode::new)
            .withItem(block -> new BatteryBlockItem(block, new Item.Properties()))   // custom item
            .addToTab(Logistics.CUSTOM_TAB.key())
            .register();

    public static final BlockEntry<PipeBlock, PipeNode> PIPE_BLOCK = Registrate
            .block(
                    Logistics.id("pipe"),
                    () -> new PipeBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).noOcclusion().dynamicShape().noOcclusion().requiresCorrectToolForDrops()))
            .network(BuiltInNetworks.ITEM_NETWORK, PipeNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.key())
            .register();

    public static void register() {

    }
}

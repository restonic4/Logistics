package com.restonic4.logistics.blocks;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.accersor.AccessorBlock;
import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.blocks.audio_station.AudioStationBlock;
import com.restonic4.logistics.blocks.audio_station.AudioStationNode;
import com.restonic4.logistics.blocks.battery.BatteryBlock;
import com.restonic4.logistics.blocks.battery.BatteryBlockItem;
import com.restonic4.logistics.blocks.battery.BatteryNode;
import com.restonic4.logistics.blocks.cable.CableBlock;
import com.restonic4.logistics.blocks.cable.CableNode;
import com.restonic4.logistics.blocks.charging_station.ChargingStationBlock;
import com.restonic4.logistics.blocks.charging_station.ChargingStationBlockEntity;
import com.restonic4.logistics.blocks.charging_station.ChargingStationNode;
import com.restonic4.logistics.blocks.computer.ComputerBlock;
import com.restonic4.logistics.blocks.computer.ComputerNode;
import com.restonic4.logistics.blocks.generator.GeneratorBlock;
import com.restonic4.logistics.blocks.generator.GeneratorNode;
import com.restonic4.logistics.blocks.lamp.LampBlock;
import com.restonic4.logistics.blocks.lamp.LampNode;
import com.restonic4.logistics.blocks.machine.MachineBlock;
import com.restonic4.logistics.blocks.machine.MachineNode;
import com.restonic4.logistics.blocks.network_connector.NetworkConnectorBlock;
import com.restonic4.logistics.blocks.network_connector.NetworkConnectorNode;
import com.restonic4.logistics.blocks.network_switch.NetworkSwitchBlock;
import com.restonic4.logistics.blocks.network_switch.NetworkSwitchNode;
import com.restonic4.logistics.blocks.redstone_reader.RedstoneReaderBlock;
import com.restonic4.logistics.blocks.redstone_reader.RedstoneReaderNode;
import com.restonic4.logistics.blocks.pipe.PipeBlock;
import com.restonic4.logistics.blocks.pipe.PipeNode;
import com.restonic4.logistics.blocks.protector.CreativeProtectorBlock;
import com.restonic4.logistics.blocks.protector.ProtectorBlock;
import com.restonic4.logistics.blocks.protector.ProtectorNode;
import com.restonic4.logistics.experiment.Sounds;
import com.restonic4.logistics.networks.BuiltInNetworks;
import com.restonic4.logistics.registry.PlatformRegistry;
import com.restonic4.logistics.registry.entries.BlockEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public class BlockRegistry {
    public static final BlockEntry<CableBlock, CableNode> CABLE_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("cable"),
                    () -> new CableBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).sound(SoundType.WOOL).noOcclusion().dynamicShape().noOcclusion().requiresCorrectToolForDrops())
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, CableNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final BlockEntry<GeneratorBlock, GeneratorNode> GENERATOR_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("generator"),
                    () -> new GeneratorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, GeneratorNode::new)
            .withItem(new Item.Properties().rarity(Rarity.EPIC))
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final BlockEntry<MachineBlock, MachineNode> MACHINE_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("machine"),
                    () -> new MachineBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, MachineNode::new)
            .withItem(new Item.Properties().rarity(Rarity.EPIC))
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final BlockEntry<BatteryBlock, BatteryNode> BATTERY_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("battery"),
                    () -> new BatteryBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, BatteryNode::new)
            .withItem(block -> new BatteryBlockItem(block, new Item.Properties()))
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final BlockEntry<NetworkConnectorBlock, NetworkConnectorNode> NETWORK_CONNECTOR_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("network_connector"),
                    () -> new NetworkConnectorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, NetworkConnectorNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final BlockEntry<ComputerBlock, ComputerNode> COMPUTER_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("computer"),
                    () -> new ComputerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, ComputerNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final BlockEntry<LampBlock, LampNode> LAMP_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("lamp"),
                    () -> new LampBlock(BlockBehaviour.Properties.copy(Blocks.REDSTONE_LAMP).lightLevel(state -> state.getValue(LampBlock.LIT) ? 15 : 0))
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, LampNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final BlockEntry<ChargingStationBlock, ChargingStationNode> CHARGING_STATION_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("charging_station"),
                    () -> new ChargingStationBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, ChargingStationNode::new)
            .withBlockEntity(ChargingStationBlockEntity::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final BlockEntry<AudioStationBlock, AudioStationNode> AUDIO_STATION_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("audio_station"),
                    () -> new AudioStationBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, AudioStationNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final BlockEntry<ProtectorBlock, ProtectorNode> PROTECTOR_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("protector"),
                    () -> new ProtectorBlock(BlockBehaviour.Properties.copy(Blocks.BEACON).noOcclusion().requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK))
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, ProtectorNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final BlockEntry<CreativeProtectorBlock, ProtectorNode> CREATIVE_PROTECTOR_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("creative_protector"),
                    () -> new CreativeProtectorBlock(BlockBehaviour.Properties.copy(Blocks.BEACON).noOcclusion().requiresCorrectToolForDrops())
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, ProtectorNode::new)
            .withItem(new Item.Properties().rarity(Rarity.EPIC))
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final BlockEntry<PipeBlock, PipeNode> PIPE_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("pipe"),
                    () -> new PipeBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).sound(SoundType.COPPER).noOcclusion().dynamicShape().noOcclusion().requiresCorrectToolForDrops())
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ITEM_NETWORK, PipeNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final BlockEntry<AccessorBlock, AccessorNode> ACCESSOR_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("accessor"),
                    () -> new AccessorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ITEM_NETWORK, AccessorNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final BlockEntry<RedstoneReaderBlock, RedstoneReaderNode> REDSTONE_READER_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("redstone_reader"),
                    () -> new RedstoneReaderBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, RedstoneReaderNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final BlockEntry<NetworkSwitchBlock, NetworkSwitchNode> NETWORK_SWITCH_BLOCK = PlatformRegistry
            .block(
                    Logistics.id("network_switch"),
                    () -> new NetworkSwitchBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().requiresCorrectToolForDrops())
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, NetworkSwitchNode::new)
            .withItem()
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static void register() {

    }
}

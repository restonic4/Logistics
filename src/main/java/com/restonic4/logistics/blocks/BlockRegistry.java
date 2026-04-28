package com.restonic4.logistics.blocks;

import com.restonic4.logistics.blocks.battery.BatteryBlock;
import com.restonic4.logistics.blocks.battery.BatteryBlockEntity;
import com.restonic4.logistics.blocks.generator.GeneratorBlock;
import com.restonic4.logistics.blocks.generator.GeneratorBlockEntity;
import com.restonic4.logistics.blocks.machine.MachineBlock;
import com.restonic4.logistics.blocks.machine.MachineBlockEntity;
import com.restonic4.logistics.blocks.pipe.PipeBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.world.level.block.Blocks;

public class BlockRegistry {
    public static final BlockRegistryEntry BASIC_PIPE = BlockRegistryEntry.register(
            "basic_pipe",
            new PipeBlock(FabricBlockSettings.copyOf(Blocks.GLASS).nonOpaque().dynamicShape().noOcclusion()),
            null
    );

    public static final BlockRegistryEntry GENERATOR_BLOCK = BlockRegistryEntry.register(
            "generator",
            new GeneratorBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).noOcclusion()),
            GeneratorBlockEntity::new
    );

    public static final BlockRegistryEntry MACHINE_BLOCK = BlockRegistryEntry.register(
            "machine",
            new MachineBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).noOcclusion()),
            MachineBlockEntity::new
    );

    public static final BlockRegistryEntry BATTERY_BLOCK = BlockRegistryEntry.register(
            "battery",
            new BatteryBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).noOcclusion()),
            BatteryBlockEntity::new
    );

    public static void register() {

    }
}

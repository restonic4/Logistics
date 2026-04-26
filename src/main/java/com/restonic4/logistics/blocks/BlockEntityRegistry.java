package com.restonic4.logistics.blocks;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.entity.BatteryBlockEntity;
import com.restonic4.logistics.blocks.entity.GeneratorBlockEntity;
import com.restonic4.logistics.blocks.entity.MachineBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntityRegistry {

    public static final BlockEntityType<GeneratorBlockEntity> GENERATOR =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Logistics.id("generator"),
                    FabricBlockEntityTypeBuilder.create(GeneratorBlockEntity::new, BlockRegistry.GENERATOR_BLOCK).build()
            );

    public static final BlockEntityType<MachineBlockEntity> MACHINE =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Logistics.id("machine"),
                    FabricBlockEntityTypeBuilder.create(MachineBlockEntity::new, BlockRegistry.MACHINE_BLOCK).build()
            );

    public static final BlockEntityType<BatteryBlockEntity> BATTERY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Logistics.id("battery"),
                    FabricBlockEntityTypeBuilder.create(BatteryBlockEntity::new, BlockRegistry.BATTERY_BLOCK).build()
            );

    public static void register() {
        // Accessing this class triggers the static fields above.
    }
}
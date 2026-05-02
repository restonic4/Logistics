package com.restonic4.logistics.compatibility.create;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.compatibility.create.blocks.motor.CreateMotorBlock;
import com.restonic4.logistics.compatibility.create.blocks.motor.CreateMotorBlockEntity;
import com.restonic4.logistics.compatibility.create.blocks.motor.CreateMotorNode;
import com.restonic4.logistics.compatibility.create.blocks.transformer.CreateTransformerBlock;
import com.restonic4.logistics.compatibility.create.blocks.transformer.CreateTransformerBlockEntity;
import com.restonic4.logistics.compatibility.create.blocks.transformer.CreateTransformerNode;
import com.restonic4.logistics.energy.NodeTypeRegistry;
import com.simibubi.create.AllBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;

// TODO: Fix create nodes, they load chunks lmao
public class CreateCompatibility {
    public static final float CONVERSION_RATE = 0.00390625f; // 256 = 1
    public static final long CONVERSION_LOSS_TICKS = 5;

    public static final ResourceLocation CREATE_MOTOR_ID = Logistics.id("create_motor");
    public static final NodeTypeRegistry.NetworkNodeType<CreateMotorNode> CREATE_MOTOR_NODE_TYPE = NodeTypeRegistry.register(CREATE_MOTOR_ID, CreateMotorNode::new);
    public static final CreateMotorBlock CREATE_MOTOR_BLOCK = new CreateMotorBlock(FabricBlockSettings.copyOf(AllBlocks.CREATIVE_MOTOR.get()));
    public static final Item CREATE_MOTOR_ITEM = new BlockItem(CREATE_MOTOR_BLOCK, new Item.Properties());
    public static final BlockEntityType<CreateMotorBlockEntity> CREATE_MOTOR_BLOCK_ENTITY_TYPE = FabricBlockEntityTypeBuilder.create(CreateMotorBlockEntity::new, CREATE_MOTOR_BLOCK).build();

    public static final ResourceLocation CREATE_TRANSFORMER_ID = Logistics.id("create_transformer");
    public static final NodeTypeRegistry.NetworkNodeType<CreateTransformerNode> CREATE_TRANSFORMER_NODE_TYPE = NodeTypeRegistry.register(CREATE_TRANSFORMER_ID, CreateTransformerNode::new);
    public static final CreateTransformerBlock CREATE_TRANSFORMER_BLOCK = new CreateTransformerBlock(FabricBlockSettings.copyOf(AllBlocks.CREATIVE_MOTOR.get()));
    public static final Item CREATE_TRANSFORMER_ITEM = new BlockItem(CREATE_TRANSFORMER_BLOCK, new Item.Properties());
    public static final BlockEntityType<CreateTransformerBlockEntity> CREATE_TRANSFORMER_BLOCK_ENTITY_TYPE = FabricBlockEntityTypeBuilder.create(CreateTransformerBlockEntity::new, CREATE_TRANSFORMER_BLOCK).build();

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, CREATE_MOTOR_ID, CREATE_MOTOR_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, CREATE_MOTOR_ID, CREATE_MOTOR_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, CREATE_MOTOR_ID, CREATE_MOTOR_BLOCK_ENTITY_TYPE);

        Registry.register(BuiltInRegistries.BLOCK, CREATE_TRANSFORMER_ID, CREATE_TRANSFORMER_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, CREATE_TRANSFORMER_ID, CREATE_TRANSFORMER_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, CREATE_TRANSFORMER_ID, CREATE_TRANSFORMER_BLOCK_ENTITY_TYPE);

        ItemGroupEvents.modifyEntriesEvent(Logistics.CUSTOM_TAB_KEY).register(entries -> {
            entries.accept(CREATE_MOTOR_ITEM);
            entries.accept(CREATE_TRANSFORMER_ITEM);
        });
    }
}

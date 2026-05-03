package com.restonic4.logistics.compatibility.create;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.compatibility.create.blocks.motor.CreateMotorBlock;
import com.restonic4.logistics.compatibility.create.blocks.motor.CreateMotorBlockEntity;
import com.restonic4.logistics.compatibility.create.blocks.motor.CreateMotorNode;
import com.restonic4.logistics.compatibility.create.blocks.transformer.CreateTransformerBlock;
import com.restonic4.logistics.compatibility.create.blocks.transformer.CreateTransformerBlockEntity;
import com.restonic4.logistics.compatibility.create.blocks.transformer.CreateTransformerNode;
import com.restonic4.logistics.networks.BuiltInNetworks;
import com.restonic4.logistics.registry.LogisticsRegistryEntry;
import com.restonic4.logistics.registry.Registrate;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllCreativeModeTabs;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;

// TODO: Fix create nodes, they load chunks lmao
public class CreateCompatibility {
    public static final float CONVERSION_RATE = 0.00390625f; // 256 = 1
    public static final long CONVERSION_LOSS_TICKS = 5;

    public static final LogisticsRegistryEntry<CreateMotorBlock, CreateMotorNode> CREATE_MOTOR = Registrate
            .block(
                    Logistics.id("create_motor"),
                    () -> new CreateMotorBlock(FabricBlockSettings.copyOf(AllBlocks.CREATIVE_MOTOR.get())))
            .network(BuiltInNetworks.ENERGY_NETWORK, CreateMotorNode::new)
            .withItem()
            .withBlockEntity(CreateMotorBlockEntity::new)
            .addToTab(Logistics.CUSTOM_TAB_KEY)
            .addToTab(AllCreativeModeTabs.BASE_CREATIVE_TAB.key())
            .register();

    public static final LogisticsRegistryEntry<CreateTransformerBlock, CreateTransformerNode> CREATE_TRANSFORMER = Registrate
            .block(
                    Logistics.id("create_transformer"),
                    () -> new CreateTransformerBlock(FabricBlockSettings.copyOf(AllBlocks.CREATIVE_MOTOR.get())))
            .network(BuiltInNetworks.ENERGY_NETWORK, CreateTransformerNode::new)
            .withItem()
            .withBlockEntity(CreateTransformerBlockEntity::new)
            .addToTab(Logistics.CUSTOM_TAB_KEY)
            .addToTab(AllCreativeModeTabs.BASE_CREATIVE_TAB.key())
            .register();

    public static void register() {

    }
}

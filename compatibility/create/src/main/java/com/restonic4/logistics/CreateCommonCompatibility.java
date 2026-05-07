package com.restonic4.logistics;

import com.restonic4.logistics.blocks.motor.CreateMotorBlock;
import com.restonic4.logistics.blocks.motor.CreateMotorBlockEntity;
import com.restonic4.logistics.blocks.motor.CreateMotorNode;
import com.restonic4.logistics.blocks.transformer.CreateTransformerBlock;
import com.restonic4.logistics.blocks.transformer.CreateTransformerBlockEntity;
import com.restonic4.logistics.blocks.transformer.CreateTransformerNode;
import com.restonic4.logistics.networks.BuiltInNetworks;
import com.restonic4.logistics.registry.PlatformRegistry;
import com.restonic4.logistics.registry.entries.BlockEntry;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;

// TODO: Fix create nodes, they load chunks lmao
public class CreateCommonCompatibility {
    public static final float CONVERSION_RATE = 0.00390625f; // 256 = 1
    public static final long CONVERSION_LOSS_TICKS = 5;

    public static final BlockEntry<CreateMotorBlock, CreateMotorNode> CREATE_MOTOR = PlatformRegistry
            .block(
                    Logistics.id("create_motor"),
                    () -> new CreateMotorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK))
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, CreateMotorNode::new)
            .withItem()
            .withBlockEntity(CreateMotorBlockEntity::new)
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .addToTab(CreateCompatibilityImpl.getBaseCreativeTab())
            .register();

    public static final BlockEntry<CreateTransformerBlock, CreateTransformerNode> CREATE_TRANSFORMER = PlatformRegistry
            .block(
                    Logistics.id("create_transformer"),
                    () -> new CreateTransformerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK))
            )
            .mineWithPickaxe().dropSelf()
            .network(BuiltInNetworks.ENERGY_NETWORK, CreateTransformerNode::new)
            .withItem()
            .withBlockEntity(CreateTransformerBlockEntity::new)
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .addToTab(CreateCompatibilityImpl.getBaseCreativeTab())
            .register();

    public static void register() {

    }

    public static boolean hasGoggleOverlay(ServerLevel serverLevel, BlockPos pos) {
        BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
        return blockEntity instanceof IHaveHoveringInformation || blockEntity instanceof IHaveGoggleInformation;
    }
}

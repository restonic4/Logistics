package com.restonic4.logistics;

import com.restonic4.logistics.blocks.motor.CreateMotorBlockEntity;
import com.restonic4.logistics.blocks.motor.CreateMotorRenderer;
import com.restonic4.logistics.blocks.transformer.CreateTransformerBlockEntity;
import com.restonic4.logistics.blocks.transformer.CreateTransformerRenderer;
import com.restonic4.logistics.registry.builders.ClientBlockBuilder;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class CreateClientCompatibility {
    public static void register() {
        ClientBlockBuilder.of(CreateCommonCompatibility.CREATE_MOTOR).renderer(CreateMotorBlockEntity.class, CreateMotorRenderer::new).register();
        SimpleBlockEntityVisualizer.builder(CreateCommonCompatibility.CREATE_MOTOR.getBlockEntityType(CreateMotorBlockEntity.class))
                .factory(OrientedRotatingVisual.<CreateMotorBlockEntity>of(AllPartialModels.SHAFT_HALF)::create)
                .skipVanillaRender(be -> false)
                .apply();

        ClientBlockBuilder.of(CreateCommonCompatibility.CREATE_TRANSFORMER).renderer(CreateTransformerBlockEntity.class, CreateTransformerRenderer::new).register();
        SimpleBlockEntityVisualizer.builder(CreateCommonCompatibility.CREATE_TRANSFORMER.getBlockEntityType(CreateTransformerBlockEntity.class))
                .factory((context, blockEntity, partialTick) -> {
                    Direction facing = blockEntity.getBlockState()
                            .getValue(BlockStateProperties.FACING)
                            .getOpposite();
                    return new OrientedRotatingVisual<>(
                            context, blockEntity, partialTick,
                            Direction.SOUTH, facing,
                            Models.partial(AllPartialModels.SHAFT_HALF)
                    );
                })
                .skipVanillaRender(be -> false)
                .apply();
    }
}

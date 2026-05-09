package com.restonic4.logistics;

import com.restonic4.logistics.blocks.motor.CreateMotorBlockEntity;
import com.restonic4.logistics.blocks.motor.CreateMotorRenderer;
import com.restonic4.logistics.registry.builders.ClientBlockBuilder;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class CreateClientCompatibility {
    public static void register() {
        ClientBlockBuilder.of(CreateCommonCompatibility.CREATE_MOTOR).renderer(CreateMotorBlockEntity.class, CreateMotorRenderer::new).register();

        SimpleBlockEntityVisualizer.builder(CreateCommonCompatibility.CREATE_MOTOR.getBlockEntityType(CreateMotorBlockEntity.class))
                .factory(OrientedRotatingVisual.<CreateMotorBlockEntity>of(AllPartialModels.SHAFT_HALF)::create)
                .skipVanillaRender(be -> false)
                .apply();
    }
}

package com.restonic4.logistics.compatibility.create;

import com.restonic4.logistics.compatibility.create.blocks.motor.CreateMotorBlockEntity;
import com.restonic4.logistics.compatibility.create.blocks.motor.CreateMotorRenderer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class CreateClientCompatibility {
    public static void register() {
        BlockEntityRenderers.register(
                CreateCompatibility.CREATE_MOTOR_BLOCK_ENTITY_TYPE,
                CreateMotorRenderer::new
        );

        SimpleBlockEntityVisualizer.builder(CreateCompatibility.CREATE_MOTOR_BLOCK_ENTITY_TYPE)
                .factory(OrientedRotatingVisual.<CreateMotorBlockEntity>of(AllPartialModels.SHAFT_HALF)::create)
                .skipVanillaRender(be -> false)
                .apply();
    }
}

package com.restonic4.logistics.blocks.transformer;

import com.restonic4.logistics.blocks.motor.CreateMotorBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class CreateTransformerRenderer extends KineticBlockEntityRenderer<CreateTransformerBlockEntity>  {
    public CreateTransformerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(CreateTransformerBlockEntity be, BlockState state) {
        Direction flipped = state.getValue(BlockStateProperties.FACING).getOpposite();
        BlockState flippedState = state.setValue(BlockStateProperties.FACING, flipped);
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, flippedState);
    }
}

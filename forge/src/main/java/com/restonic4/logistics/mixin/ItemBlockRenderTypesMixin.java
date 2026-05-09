package com.restonic4.logistics.mixin;

import com.restonic4.logistics.registry.ClientBlockRegistry;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {
    @Shadow @Final private static Map<Block, RenderType> TYPE_BY_BLOCK;

    // I truly hate forge so much
    // I hate forge, I hate forge, I hate forge, I hate forge, I hate forge, I hate forge, I hate forge, I hate forge, I hate forge, I hate forge, I hate forge...
    @Inject(method = "getRenderLayers", at = @At("HEAD"), cancellable = true, remap = false)
    private static void logistics$overrideForgeRenderLayers(BlockState state, CallbackInfoReturnable<ChunkRenderTypeSet> cir) {
        if (!ClientBlockRegistry.areRenderTypesInjected()) {
            ClientBlockRegistry.markRenderTypesAsInjected(TYPE_BY_BLOCK);
        }

        RenderType customType = ClientBlockRegistry.BLOCKS_RENDER_TYPES.get(state.getBlock());
        if (customType != null) {
            cir.setReturnValue(net.minecraftforge.client.ChunkRenderTypeSet.of(customType));
        }
    }
}

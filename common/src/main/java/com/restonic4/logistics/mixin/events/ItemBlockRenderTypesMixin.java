package com.restonic4.logistics.mixin.events;

import com.restonic4.logistics.registry.ClientBlockRegistry;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {
    @Shadow @Final private static Map<Block, RenderType> TYPE_BY_BLOCK;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void logistics$onCLInit(CallbackInfo ci) {
        if (!ClientBlockRegistry.areRenderTypesInjected()) {
            ClientBlockRegistry.markRenderTypesAsInjected(TYPE_BY_BLOCK);
        }
    }

    @Inject(method = "getChunkRenderType", at = @At("HEAD"), cancellable = true)
    private static void logistics$injectLateRenderTypes(BlockState state, CallbackInfoReturnable<RenderType> cir) {
        if (!ClientBlockRegistry.areRenderTypesInjected()) {
            ClientBlockRegistry.markRenderTypesAsInjected(TYPE_BY_BLOCK);
        }

        RenderType type = ClientBlockRegistry.BLOCKS_RENDER_TYPES.get(state.getBlock());
        if (type != null) {
            cir.setReturnValue(type);
        }
    }
}

package com.restonic4.logistics.mixin.events;

import com.restonic4.logistics.events.ChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public class LevelChunkMixin {
    @Inject(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN")
    )
    private void logistics$onBlockStateChanged(BlockPos pos, BlockState newState, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        BlockState oldState = cir.getReturnValue();
        if (oldState == null) return; // no change happened
        if (oldState == newState) return; // same state object
        if (oldState.getBlock() != newState.getBlock()) return; // different block

        LevelChunk chunk = (LevelChunk) (Object) this;
        ChunkEvents.BLOCKSTATE_CHANGED.invoker().onEvent(chunk, pos, oldState, newState, isMoving);
    }
}

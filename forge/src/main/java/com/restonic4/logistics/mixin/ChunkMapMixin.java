package com.restonic4.logistics.mixin;

import com.restonic4.logistics.events.ChunkEvents;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    @Final @Shadow ServerLevel level;

    @Inject(
            method = "lambda$protoChunkToFullChunk$34(Lnet/minecraft/server/level/ChunkHolder;Lnet/minecraft/world/level/chunk/ChunkAccess;)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            at = @At("TAIL"),
            remap = false
    )
    private void logistics$onChunkAdd(ChunkHolder chunkHolder, ChunkAccess chunkAccess, CallbackInfoReturnable<ChunkAccess> cir) {
        //ChunkEvents.LOAD.invoker().onEvent(this.level, chunkHolder.getPos());
    }

    @Inject(
            method = "lambda$scheduleUnload$14(Lnet/minecraft/server/level/ChunkHolder;Ljava/util/concurrent/CompletableFuture;JLnet/minecraft/world/level/chunk/ChunkAccess;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/LevelChunk;setLoaded(Z)V",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private void logistics$onChunkRemove(ChunkHolder chunkHolder, CompletableFuture<ChunkAccess> futureChunkAccess, long pos, ChunkAccess chunkAccess, CallbackInfo ci) {
        ChunkEvents.UNLOAD.invoker().onEvent(this.level, (LevelChunk) chunkAccess);
    }
}

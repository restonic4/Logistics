package com.restonic4.logistics.mixin.events;

import com.restonic4.logistics.events.ChunkEvents;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    @Final @Shadow ServerLevel level;

    @Inject(
            method = "onFullChunkStatusChange(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/server/level/FullChunkStatus;)V",
            at = @At("HEAD")
    )
    private void logistics$onFullChunkStatusChange(
            ChunkPos chunkPos,
            FullChunkStatus fullChunkStatus,
            CallbackInfo ci
    ) {
        if (fullChunkStatus == FullChunkStatus.INACCESSIBLE) {
            ChunkEvents.UNLOAD.invoker().onEvent(level, chunkPos);
        } else if (fullChunkStatus == FullChunkStatus.FULL) {
            ChunkEvents.LOAD.invoker().onEvent(level, chunkPos);
        }
    }
}

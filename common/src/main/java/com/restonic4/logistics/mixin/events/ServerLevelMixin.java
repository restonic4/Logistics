package com.restonic4.logistics.mixin.events;

import com.restonic4.logistics.events.ChunkEvents;
import com.restonic4.logistics.events.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Shadow @Final private MinecraftServer server;

    @Inject(method = "tick", at = @At("HEAD"))
    private void logistics$startTick(BooleanSupplier $$0, CallbackInfo ci) {
        ServerTickEvents.START.invoker().onEvent(this.server);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void logistics$endTick(BooleanSupplier $$0, CallbackInfo ci) {
        ServerTickEvents.END.invoker().onEvent(this.server);
    }

    @Inject(method = "startTickingChunk", at = @At("HEAD"))
    private void logistics$onChunkStartTicks(LevelChunk levelChunk, CallbackInfo ci) {
        ChunkEvents.LOAD.invoker().onEvent((ServerLevel) (Object) this, levelChunk);
    }
}

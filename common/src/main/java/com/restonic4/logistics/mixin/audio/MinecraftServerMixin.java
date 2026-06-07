package com.restonic4.logistics.mixin.audio;

import com.restonic4.logistics.audio.server.ServerAudioManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "tickServer", at = @At("RETURN"))
    private void logistics$onServerTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ServerAudioManager.getInstance().tick();
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void logistics$onServerStop(CallbackInfo ci) {
        ServerAudioManager.getInstance().stopAll();
    }
}
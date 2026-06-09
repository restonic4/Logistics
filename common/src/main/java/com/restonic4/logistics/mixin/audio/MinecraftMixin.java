package com.restonic4.logistics.mixin.audio;

import com.restonic4.logistics.audio.ClientAudioManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void logistics$onClientTick(CallbackInfo ci) {
        ClientAudioManager.tick();
    }

    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("TAIL"))
    private void logistics$onClearLevel(net.minecraft.client.gui.screens.Screen screen, CallbackInfo ci) {
        ClientAudioManager.clear();
    }
}
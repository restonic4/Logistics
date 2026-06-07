package com.restonic4.logistics.mixin.audio;

import com.restonic4.logistics.audio.client.ClientAudioManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void logistics$onClientTick(CallbackInfo ci) {
        ClientAudioManager.getInstance().tick();
    }
}
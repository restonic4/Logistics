package com.restonic4.logistics.mixin.experiment;

import com.restonic4.logistics.experiment.ScreenShakeManager;
import com.restonic4.logistics.experiment.ShockwaveManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(at = @At("HEAD"), method = "tick")
    private void onStartTick(CallbackInfo info) {

    }

    @Inject(at = @At("RETURN"), method = "tick")
    private void onEndTick(CallbackInfo info) {
        Minecraft client = (Minecraft) (Object) this;
        if (client.level != null) ShockwaveManager.clientTick(client.level);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onClientTick(CallbackInfo ci) {
        ScreenShakeManager.getInstance().tick();
    }
}
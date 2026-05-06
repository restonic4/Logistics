package com.restonic4.logistics.mixin;

import com.restonic4.logistics.events.RenderCallbacks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeGui.class)
public class ForgeGuiMixin {
    // I hate Forge so much, why the actual heck would you completely overwrite an entire class. What's this bullshit?

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V"))
    public void logistics$render(GuiGraphics drawContext, float tickDelta, CallbackInfo callbackInfo) {
        RenderCallbacks.ON_HUD_RENDERED.invoker().onEvent(drawContext, tickDelta);
    }
}

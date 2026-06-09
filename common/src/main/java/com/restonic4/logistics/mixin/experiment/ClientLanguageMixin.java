package com.restonic4.logistics.mixin.experiment;

import com.restonic4.logistics.Constants;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public class ClientLanguageMixin {
    @Inject(method = "getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void makeTranslationsGreen(String id, String fallback, CallbackInfoReturnable<String> cir) {
        if (Constants.isDebug()) cir.setReturnValue("§aCORRECT");
    }
}
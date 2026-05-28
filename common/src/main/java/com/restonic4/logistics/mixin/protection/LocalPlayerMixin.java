package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.data_types.ClientProtectionCache;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    // ==================== chorus_fruit ====================
    @Inject(method = "startUsingItem", at = @At("HEAD"), cancellable = true)
    private void onStartUsingItem(InteractionHand hand, CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (!self.level().isClientSide()) return;

        ItemStack stack = self.getItemInHand(hand);
        if (!stack.is(Items.CHORUS_FRUIT)) return;

        FlagData fd = ClientProtectionCache.getFlagState(
                self.level().dimension().location(), self.blockPosition(), self, "chorus_fruit");
        if (fd != null && fd.enabled()) ci.cancel();
    }

    // ==================== sneaking – HEAD so pose updates this tick ====================
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickSneaking(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (!self.level().isClientSide()) return;
        if (!self.input.shiftKeyDown) return;

        FlagData fd = ClientProtectionCache.getFlagState(
                self.level().dimension().location(), self.blockPosition(), self, "sneaking");
        if (fd == null || !fd.enabled()) return;

        self.input.shiftKeyDown = false;
        self.setShiftKeyDown(false);
    }

    // ==================== walk_in ====================
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickWalkIn(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (!self.level().isClientSide()) return;

        FlagData fd = ClientProtectionCache.getFlagState(
                self.level().dimension().location(), self.blockPosition(), self, "walk_in");
        if (fd == null || !fd.enabled()) return;

        if (self.isSprinting()) self.setSprinting(false);
    }
}
package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.ClientProtectionCache;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    // ==================== sneaking – HEAD so pose updates this tick ====================
    @Inject(method = "tick", at = @At("TAIL"))
    private void logistics$onClientTickTail(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;

        FlagData fd = ProtectionMixinUtils.getFlag(
                player.level(), player.blockPosition(), player, "sneaking"
        );
        if (!ProtectionMixinUtils.isZoneDenied(fd)) return;

        try {
            if (ActionType.valueOf(fd.actionType()) != ActionType.DENY) return;
        } catch (IllegalArgumentException e) { return; }

        // 1. Starve the keyboard input so next tick doesn't start a new sneak
        player.input.shiftKeyDown = false;

        // 2. Wipe the client-side crouch field EVERY TICK.
        //    This is what LocalPlayer uses for ledge safety and camera height.
        player.crouching = false;

        // 3. Wipe the shared entity flag
        player.setShiftKeyDown(false);

        // 4. Force pose up if room
        if (player.getPose() == Pose.CROUCHING
                && player.canEnterPose(Pose.STANDING)) {
            player.setPose(Pose.STANDING);
            player.refreshDimensions();
        }
    }

    // ==================== item_drop ====================
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void onDrop(boolean dropAll, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (!self.level().isClientSide()) return;

        FlagData fd = ClientProtectionCache.getFlagState(
                self.level().dimension().location(), self.blockPosition(), self, "item_drop");
        if (fd != null && fd.enabled()) cir.setReturnValue(false);
    }
}
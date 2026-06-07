package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "setShiftKeyDown", at = @At("HEAD"), cancellable = true)
    private void logistics$blockSneakFlag(boolean shifting, CallbackInfo ci) {
        if (!shifting) return;

        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;
        if (!player.canEnterPose(Pose.STANDING)) return;

        FlagData fd = ProtectionMixinUtils.getFlag(player.level(), player.blockPosition(), player, "sneaking");
        if (!ProtectionMixinUtils.isZoneActive(player.level(), player.blockPosition(), fd)) return;

        try {
            if (ActionType.valueOf(fd.actionType()) == ActionType.DENY) {
                ci.cancel();
            }
        } catch (IllegalArgumentException ignored) {}
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void onIsInvaulnerableTo(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide()) return;

        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player player)) return;
        if (self instanceof Player) return; // PvP handled elsewhere

        FlagData fd = ServerProtectionCache.getFlagState(player.level().dimension().location(), self.blockPosition(), player, "attack_entities");
        if (!ProtectionMixinUtils.isZoneActive(player.level(), self.blockPosition(), fd)) return;
        cir.setReturnValue(true);
    }
}

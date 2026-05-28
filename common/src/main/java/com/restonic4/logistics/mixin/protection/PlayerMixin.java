package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.computer.screen.ProtectionTabDummyData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
    @Inject(
            method = "hurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cancelPlayerCombat(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }

        Entity attacker = source.getEntity();

        if (attacker instanceof Player attackerPlayer && attackerPlayer != self) {
            // Get the flag state for "pvp" at the victim's current position
            ProtectionTabDummyData.FlagState pvpState = ProtectionTabDummyData.getFlagState(
                    self.level().dimension().location(),
                    self.blockPosition(),
                    attackerPlayer,
                    "pvp"
            );

            // If pvpState is null, it's the wild wilderness -> proceed with regular vanilla combat
            if (pvpState != null && pvpState.enabled) {

                // 1. If it's outright DENIED, cancel the attack entirely
                if (pvpState.action == ProtectionTabDummyData.ActionType.DENY) {
                    cir.setReturnValue(false);
                    return;
                }

                // 2. If it's a MESSAGE trap, warn the attacker and cancel the attack
                if (pvpState.action == ProtectionTabDummyData.ActionType.MESSAGE) {
                    if (pvpState.message != null && !pvpState.message.isEmpty()) {
                        attackerPlayer.sendSystemMessage(Component.literal(pvpState.message));
                    }
                    cir.setReturnValue(false);
                    return;
                }

                // 3. If it's a DAMAGE trap, turn the damage back onto the attacker and cancel the attack
                if (pvpState.action == ProtectionTabDummyData.ActionType.DAMAGE) {
                    attackerPlayer.hurt(
                            self.level().damageSources().generic(),
                            (float) pvpState.damageValue
                    );
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
    }
}
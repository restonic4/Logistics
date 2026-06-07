package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        String specificFlag = null;
        boolean isEnvironmental = false;

        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.FALLING_BLOCK) || source.is(DamageTypes.STALAGMITE)) {
            specificFlag = "fall_damage";
            isEnvironmental = true;
        } else if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.LAVA)) {
            specificFlag = "fire_damage";
            isEnvironmental = true;
        } else if (source.is(DamageTypes.GENERIC) || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.STARVE) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.DRY_OUT)
                || source.is(DamageTypes.FREEZE) || source.is(DamageTypes.SONIC_BOOM) || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.THORNS)) {
            isEnvironmental = true;
        }

        if (specificFlag == null && !source.is(DamageTypes.PLAYER_ATTACK) && !source.is(DamageTypes.MOB_ATTACK)
                && !source.is(DamageTypes.ARROW) && !source.is(DamageTypes.TRIDENT)
                && !source.is(DamageTypes.FIREBALL) && !source.is(DamageTypes.WITHER_SKULL)) {
            isEnvironmental = true;
        }

        // Check specific flag first (e.g., fall_damage, fire_damage)
        if (specificFlag != null) {
            FlagData fd = ServerProtectionCache.getFlagState(player.level().dimension().location(), player.blockPosition(), player, specificFlag);
            if (ProtectionMixinUtils.isZoneActive(player.level(), player.blockPosition(), fd)) {
                ProtectionMixinUtils.handle(player, fd, cir);
                if (cir.isCancelled()) return;
            }
        }

        // Check lose_health flag for all environmental damage (including fall and fire)
        if (isEnvironmental) {
            FlagData fd = ServerProtectionCache.getFlagState(player.level().dimension().location(), player.blockPosition(), player, "lose_health");
            if (ProtectionMixinUtils.isZoneActive(player.level(), player.blockPosition(), fd)) {
                ProtectionMixinUtils.handle(player, fd, cir);
            }
        }
    }
}
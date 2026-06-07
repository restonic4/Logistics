package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {

    @Shadow @Final public Level level;

    /**
     * Filters out blocks inside protected areas right before they are detonated.
     */
    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void onFinalizeExplosion(boolean spawnParticles, CallbackInfo ci) {
        if (this.level == null || this.level.isClientSide()) return;

        Explosion explosion = (Explosion) (Object) this;
        LivingEntity indirectSource = explosion.getIndirectSourceEntity();
        Player player = (indirectSource instanceof Player p) ? p : null;

        // Remove any block positions from the explosion's blast radius that evaluate to DENY
        List<BlockPos> affectedBlocks = explosion.getToBlow();
        affectedBlocks.removeIf(blockPos -> logistics$isProtected(blockPos, player, indirectSource));
    }

    /**
     * Prevents entities (mobs/players) inside a protected area from taking explosion damage.
     */
    @Redirect(
            method = "explode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean redirectEntityHurt(Entity entity, DamageSource source, float amount) {
        if (this.level != null && !this.level.isClientSide()) {
            Explosion explosion = (Explosion) (Object) this;
            LivingEntity indirectSource = explosion.getIndirectSourceEntity();
            Player player = (indirectSource instanceof Player p) ? p : null;

            if (logistics$isProtected(entity.blockPosition(), player, indirectSource)) {
                return false; // Cancels the damage completely
            }
        }
        return entity.hurt(source, amount);
    }

    /**
     * Prevents entities inside a protected area from being pushed/knocked back by explosions.
     */
    @Redirect(
            method = "explode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    private void redirectSetDeltaMovement(Entity entity, Vec3 motion) {
        if (this.level != null && !this.level.isClientSide()) {
            Explosion explosion = (Explosion) (Object) this;
            LivingEntity indirectSource = explosion.getIndirectSourceEntity();
            Player player = (indirectSource instanceof Player p) ? p : null;

            if (logistics$isProtected(entity.blockPosition(), player, indirectSource)) {
                return; // Bypass changing the entity's velocity vector
            }
        }
        entity.setDeltaMovement(motion);
    }

    /**
     * Helper method to centralize protection logic evaluations per BlockPos.
     * Evaluates both general explosion rules and specific mob grief rules.
     */
    @Unique
    private boolean logistics$isProtected(BlockPos pos, Player player, LivingEntity indirectSource) {
        // 1. Global Check: If 'explosions' flag is set to DENY, block it immediately
        if (logistics$checkFlag(pos, player, "explosions")) {
            return true;
        }

        // 2. Contextual Check: If source is a Mob (Creeper, Ghast, etc.), also check 'mob_grief'
        if (indirectSource instanceof Mob) {
            return logistics$checkFlag(pos, player, "mob_grief");
        }

        return false;
    }

    /**
     * Isolated single-flag cache checker utility.
     */
    @Unique
    private boolean logistics$checkFlag(BlockPos pos, Player player, String flagId) {
        FlagData fd = ServerProtectionCache.getFlagState(this.level.dimension().location(), pos, player, flagId);
        if (!(ProtectionMixinUtils.isZoneActive(level, pos, fd))) return false;

        try {
            ActionType action = ActionType.valueOf(fd.actionType());
            return action == ActionType.DENY;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
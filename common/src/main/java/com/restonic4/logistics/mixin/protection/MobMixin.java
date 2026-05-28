package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobMixin {

    @Inject(method = "checkMobSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void onCheckSpawn(EntityType<? extends Mob> type, LevelAccessor levelAccessor,
                                     MobSpawnType spawnType, BlockPos pos, RandomSource random,
                                     CallbackInfoReturnable<Boolean> cir) {
        if (!(levelAccessor instanceof ServerLevel level)) return;
        if (level.isClientSide()) return;

        FlagData fd = ServerProtectionCache.getFlagState(
                level.dimension().location(), pos, null, "mob_spawning");
        if (fd == null || !fd.enabled()) return;

        try {
            ActionType action = ActionType.valueOf(fd.actionType());
            if (action == ActionType.DENY || action == ActionType.MESSAGE || action == ActionType.DAMAGE) cir.setReturnValue(false);
        } catch (IllegalArgumentException ignored) {}
    }
}
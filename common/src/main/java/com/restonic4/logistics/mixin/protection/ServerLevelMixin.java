package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public class ServerLevelMixin {

    @Inject(method = "isValidPositionForMob", at = @At("HEAD"), cancellable = true)
    private static void onIsValidPosition(ServerLevel level, Mob mob, double d, CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide()) return;

        BlockPos pos = mob.blockPosition();
        FlagData fd = ServerProtectionCache.getFlagState(level.dimension().location(), pos, null, "mob_spawning");
        if (!ProtectionMixinUtils.isZoneActive(level, pos, fd)) return;

        try {
            ActionType action = ActionType.valueOf(fd.actionType());
            if (action == ActionType.DENY || action == ActionType.MESSAGE || action == ActionType.DAMAGE) cir.setReturnValue(false);
        } catch (IllegalArgumentException ignored) {}
    }
}
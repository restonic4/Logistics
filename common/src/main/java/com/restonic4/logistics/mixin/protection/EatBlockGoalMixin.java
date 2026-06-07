package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EatBlockGoal.class)
public class EatBlockGoalMixin {
    @Shadow
    @Final
    private Mob mob;

    @Shadow
    @Final
    private Level level;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;blockPosition()Lnet/minecraft/core/BlockPos;"), cancellable = true)
    private void onEat(CallbackInfo ci) {
        if (level.isClientSide()) return;

        FlagData fd = ServerProtectionCache.getFlagState(level.dimension().location(), mob.blockPosition(), null, "mob_grief");
        if (!ProtectionMixinUtils.isZoneActive(level, mob.blockPosition(), fd)) return;

        ci.cancel();
    }
}

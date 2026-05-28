package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public class FireBlockMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (level.isClientSide()) return;

        FlagData fd = ServerProtectionCache.getFlagState(
                level.dimension().location(), pos, null, "fire_spread");
        if (fd == null || !fd.enabled()) return;

        try {
            ActionType action = ActionType.valueOf(fd.actionType());
            if (action == ActionType.DENY || action == ActionType.MESSAGE || action == ActionType.DAMAGE) ci.cancel();
        } catch (IllegalArgumentException ignored) {}
    }
}
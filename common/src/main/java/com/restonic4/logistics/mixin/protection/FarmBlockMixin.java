package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmBlock.class)
public class FarmBlockMixin {

    @Inject(method = "fallOn", at = @At("HEAD"), cancellable = true)
    private void onFallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance, CallbackInfo ci) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;

        FlagData fd = ServerProtectionCache.getFlagState(
                level.dimension().location(), pos, player, "trample_crops");
        ProtectionMixinUtils.handle(player, fd, ci);
    }
}
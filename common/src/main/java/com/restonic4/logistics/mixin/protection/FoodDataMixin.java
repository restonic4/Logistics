package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public class FoodDataMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(Player player, CallbackInfo ci) {
        if (player.level().isClientSide()) return;

        FlagData fd = ServerProtectionCache.getFlagState(player.level().dimension().location(), player.blockPosition(), player, "hunger");
        if (!ProtectionMixinUtils.isZoneActive(player.level(), player.blockPosition(), fd)) return;
        if (!ProtectionMixinUtils.isDenied(fd)) return;

        try {
            ActionType action = ActionType.valueOf(fd.actionType());
            if (action == ActionType.DENY || action == ActionType.MESSAGE || action == ActionType.DAMAGE) ci.cancel();
        } catch (IllegalArgumentException ignored) {}
    }
}
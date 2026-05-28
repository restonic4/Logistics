package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void onPlayerTouch(Player player, CallbackInfo ci) {
        if (player.level().isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer)) return;

        FlagData fd = ServerProtectionCache.getFlagState(
                player.level().dimension().location(), player.blockPosition(), player, "item_pickup");
        ProtectionMixinUtils.handle(player, fd, ci);
    }
}
package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ChorusFruitItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChorusFruitItem.class)
public class ChorusFruitItemMixin {

    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void onFinishUsing(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide() || !(entity instanceof Player player) || !(player instanceof ServerPlayer)) return;

        FlagData fd = ServerProtectionCache.getFlagState(
                level.dimension().location(), player.blockPosition(), player, "chorus_fruit");
        ProtectionMixinUtils.handleStack(player, fd, cir, stack);
    }
}
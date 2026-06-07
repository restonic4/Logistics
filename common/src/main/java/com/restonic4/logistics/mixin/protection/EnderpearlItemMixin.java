package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderpearlItem.class)
public class EnderpearlItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (player == null) return;

        FlagData fd = ProtectionMixinUtils.getFlag(level, player.blockPosition(), player, "ender_pearl");
        if (!(ProtectionMixinUtils.isZoneActive(level, player.blockPosition(), fd))) return;
        if (!ProtectionMixinUtils.isDenied(fd)) return;

        ItemStack stack = player.getItemInHand(hand);
        ProtectionMixinUtils.handleReturn(player, fd, cir, InteractionResultHolder.fail(stack));
    }
}
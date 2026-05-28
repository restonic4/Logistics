package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.server.level.ServerPlayer;
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
        if (level.isClientSide() || !(player instanceof ServerPlayer)) return;

        FlagData fd = ServerProtectionCache.getFlagState(
                level.dimension().location(), player.blockPosition(), player, "ender_pearl");
        if (!ProtectionMixinUtils.isDenied(fd)) return;

        ActionType action = ActionType.valueOf(fd.actionType());
        ItemStack stack = player.getItemInHand(hand);
        switch (action) {
            case DENY -> cir.setReturnValue(InteractionResultHolder.fail(stack));
            case MESSAGE -> {
                ProtectionMixinUtils.message(player, fd);
                cir.setReturnValue(InteractionResultHolder.fail(stack));
            }
            case DAMAGE -> {
                ProtectionMixinUtils.damage(player, fd);
                cir.setReturnValue(InteractionResultHolder.fail(stack));
            }
        }
    }
}
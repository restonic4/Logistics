package com.restonic4.logistics.mixin.protection;

import com.llamalad7.mixinextras.sugar.Local;
import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public class BucketItemMixin {

    @Inject(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            cancellable = true
    )
    private void onUse(
            Level level,
            Player player,
            InteractionHand interactionHand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir,
            @Local BlockHitResult hitResult
    ) {
        BlockPos pos = hitResult.getBlockPos();
        ItemStack itemStack = player.getItemInHand(interactionHand);

        FlagData fd = ProtectionMixinUtils.getFlag(level, pos, player, "use_buckets");
        if (!ProtectionMixinUtils.isZoneActive(level, pos, fd)) return;

        ActionType actionType = ProtectionMixinUtils.getActionType(fd);
        switch (actionType) {
            case DENY -> cir.setReturnValue(InteractionResultHolder.pass(itemStack));
            case MESSAGE -> ProtectionMixinUtils.message(player, fd);
            case DAMAGE -> ProtectionMixinUtils.damage(player, fd);
        }
    }
}
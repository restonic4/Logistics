package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public class FlintAndSteelItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void logistics$onUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == null) return;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        FlagData fd = ProtectionMixinUtils.getFlag(level, pos, player, "place_blocks");
        if (ProtectionMixinUtils.isZoneActive(level, pos, fd) && ProtectionMixinUtils.isDenied(fd)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}

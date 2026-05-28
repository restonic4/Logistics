package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.ClientProtectionCache;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void onPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return;

        if (level.isClientSide()) {
            if (!(player instanceof LocalPlayer localPlayer)) return;
            FlagData fd = ClientProtectionCache.getFlagState(
                    localPlayer.level().dimension().location(), pos, localPlayer, "place_blocks");
            if (fd != null && fd.enabled()) cir.setReturnValue(InteractionResult.FAIL);
        } else {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            FlagData fd = ServerProtectionCache.getFlagState(
                    level.dimension().location(), pos, serverPlayer, "place_blocks");
            ProtectionMixinUtils.handleResult(serverPlayer, fd, cir);
        }
    }
}
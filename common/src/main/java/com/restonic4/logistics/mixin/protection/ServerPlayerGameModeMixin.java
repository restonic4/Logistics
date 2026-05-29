package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = ((ServerPlayerGameMode) (Object) this).player;
        if (player == null) return;
        ProtectionMixinUtils.handle(player, pos, "break_blocks", cir);
    }

    /**
     * Handles block interactions on right-click. Note that place_blocks is intentionally
     * NOT checked here — we only want to prevent actual block placement, not interactions
     * with existing blocks (e.g., opening chests while holding dirt). BlockItemMixin
     * handles placement prevention at BlockItem.place(), which is called after interactions
     * are attempted.
     *
     * Exception order matters:
     * 1. use_buckets — if explicitly disabled, allow; if enabled, deny
     * 2. open_containers — if explicitly disabled, allow; if enabled, deny
     * 3. block_interaction — general catch-all for remaining block interactions
     */
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (player == null) return;
        BlockPos pos = hitResult.getBlockPos();

        // Exception: open_containers
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Container) {
            FlagData fd = ServerProtectionCache.getFlagState(level.dimension().location(), pos, player, "open_containers");
            if (fd != null && !fd.enabled()) return; // explicitly allowed
            if (fd != null && fd.enabled()) {
                ProtectionMixinUtils.handleResult(player, fd, cir);
                return;
            }
        }

        // General block_interaction — handles everything else (levers, doors, etc.)
        // place_blocks is NOT here; it lives in BlockItemMixin to avoid blocking interactions
        FlagData fd = ServerProtectionCache.getFlagState(level.dimension().location(), pos, player, "block_interaction");
        ProtectionMixinUtils.handleResult(player, fd, cir);
    }
}
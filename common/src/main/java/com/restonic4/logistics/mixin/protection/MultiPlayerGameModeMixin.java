package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.data_types.ClientProtectionCache;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    // break_blocks: treat like bedrock client-side
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.level().isClientSide()) return;

        FlagData fd = ClientProtectionCache.getFlagState(
                player.level().dimension().location(), pos, player, "break_blocks");
        if (fd != null && fd.enabled()) cir.setReturnValue(false);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onContinueDestroyBlock(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.level().isClientSide()) return;

        FlagData fd = ClientProtectionCache.getFlagState(
                player.level().dimension().location(), pos, player, "break_blocks");
        if (fd != null && fd.enabled()) cir.setReturnValue(false);
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.level().isClientSide()) return;

        FlagData fd = ClientProtectionCache.getFlagState(
                player.level().dimension().location(), pos, player, "break_blocks");
        if (fd != null && fd.enabled()) cir.setReturnValue(false);
    }

    /**
     * Handles client-side block interactions on right-click. place_blocks is intentionally
     * NOT checked here — we only prevent actual block placement, not interactions with
     * existing blocks. BlockItemMixin handles placement prevention at BlockItem.place().
     *
     * Exception order:
     * 1. use_buckets
     * 2. open_containers
     * 3. block_interaction
     */
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                             CallbackInfoReturnable<InteractionResult> cir) {
        if (player == null || !player.level().isClientSide()) return;

        BlockPos pos = hitResult.getBlockPos();
        ItemStack stack = player.getItemInHand(hand);
        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();
        boolean isBucket = itemId.contains("bucket") || itemId.contains("_bucket");

        // Exception: use_buckets
        if (isBucket) {
            FlagData fd = ClientProtectionCache.getFlagState(
                    player.level().dimension().location(), pos, player, "use_buckets");
            if (fd != null && !fd.enabled()) return; // explicitly allowed
            if (fd != null && fd.enabled()) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }
        }

        // Exception: open_containers
        BlockEntity be = player.level().getBlockEntity(pos);
        if (be instanceof Container) {
            FlagData fd = ClientProtectionCache.getFlagState(
                    player.level().dimension().location(), pos, player, "open_containers");
            if (fd != null && !fd.enabled()) return; // explicitly allowed
            if (fd != null && fd.enabled()) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }
        }

        // General block_interaction
        // place_blocks is NOT here; it lives in BlockItemMixin to avoid blocking interactions
        FlagData fd = ClientProtectionCache.getFlagState(
                player.level().dimension().location(), pos, player, "block_interaction");
        if (fd != null && fd.enabled()) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void onHandleInventoryMouseClick(int $$0, int $$1, int $$2, ClickType $$3, Player $$4, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.level().isClientSide()) return;

        boolean isThrow = $$3 == ClickType.THROW;
        boolean isCursorDrop = $$3 == ClickType.PICKUP && $$1 == -999 && !player.containerMenu.getCarried().isEmpty();

        if (!isThrow && !isCursorDrop) return;

        FlagData fd = ClientProtectionCache.getFlagState(
                player.level().dimension().location(), player.blockPosition(), player, "item_drop");
        if (fd != null && fd.enabled()) {
            ci.cancel();
        }
    }
}
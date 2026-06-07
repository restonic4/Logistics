package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.computer.ComputerBlock;
import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    // break_blocks: treat like bedrock client-side
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.level().isClientSide()) return;

        FlagData fd = ClientProtectionCache.getFlagState(player.level().dimension().location(), pos, player, "break_blocks");
        if (!ProtectionMixinUtils.isZoneActive(player.level(), pos, fd)) return;
        cir.setReturnValue(false);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onContinueDestroyBlock(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.level().isClientSide()) return;

        FlagData fd = ClientProtectionCache.getFlagState(player.level().dimension().location(), pos, player, "break_blocks");
        if (!ProtectionMixinUtils.isZoneActive(player.level(), pos, fd)) return;
        cir.setReturnValue(false);
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.level().isClientSide()) return;

        FlagData fd = ClientProtectionCache.getFlagState(player.level().dimension().location(), pos, player, "break_blocks");
        if (!ProtectionMixinUtils.isZoneActive(player.level(), pos, fd)) return;
        cir.setReturnValue(false);
    }

    @Redirect(
            method = "performUseItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult logistics$interceptBlockUseClient(BlockState state, Level level, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        Block block = state.getBlock();

        if (block instanceof ComputerBlock) {
            return state.use(level, player, hand, hitResult);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Container) {
            FlagData fd = ClientProtectionCache.getFlagState(level.dimension().location(), pos, player, "open_containers");
            if (ProtectionMixinUtils.isZoneActive(level, pos, fd) && ProtectionMixinUtils.isDenied(fd)) {
                return InteractionResult.PASS;
            }
            return state.use(level, player, hand, hitResult);
        }

        FlagData fd = ClientProtectionCache.getFlagState(level.dimension().location(), pos, player, "block_interaction");
        if (ProtectionMixinUtils.isZoneActive(level, pos, fd) && ProtectionMixinUtils.isDenied(fd)) {
            return InteractionResult.PASS;
        }

        return state.use(level, player, hand, hitResult);
    }

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void onHandleInventoryMouseClick(int $$0, int $$1, int $$2, ClickType $$3, Player $$4, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.level().isClientSide()) return;

        boolean isThrow = $$3 == ClickType.THROW;
        boolean isCursorDrop = $$3 == ClickType.PICKUP && $$1 == -999 && !player.containerMenu.getCarried().isEmpty();

        if (!isThrow && !isCursorDrop) return;

        FlagData fd = ClientProtectionCache.getFlagState(player.level().dimension().location(), player.blockPosition(), player, "item_drop");
        if (!ProtectionMixinUtils.isZoneActive(player.level(), player.blockPosition(), fd)) return;

        ci.cancel();
    }
}
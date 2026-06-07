package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.computer.ComputerBlock;
import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = ((ServerPlayerGameMode) (Object) this).player;
        if (player == null) return;

        FlagData fd = ProtectionMixinUtils.getServerFlag(player.level(), pos, player, "break_blocks");
        if (!ProtectionMixinUtils.isZoneActive(player.level(), pos, fd)) return;

        ProtectionMixinUtils.handle(player, fd, cir);
    }

    @Redirect(
            method = "useItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult logistics$interceptBlockUse(BlockState state, Level level, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        Block block = state.getBlock();

        // Exception: computer blocks always handle their own logic
        if (block instanceof ComputerBlock) {
            return state.use(level, player, hand, hitResult);
        }

        // Exception: containers are governed by the open_containers flag
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Container) {
            FlagData fd = ServerProtectionCache.getFlagState(level.dimension().location(), pos, player, "open_containers");

            if (ProtectionMixinUtils.isZoneActive(level, pos, fd) && ProtectionMixinUtils.isDenied(fd)) {
                ProtectionMixinUtils.message(player, fd);
                ProtectionMixinUtils.damage(player, fd);
                return InteractionResult.PASS; // suppress container open, let item-use proceed
            }
            return state.use(level, player, hand, hitResult);
        }

        // block_interaction: stop the block from reacting, but do NOT kill item-use (placement)
        FlagData fd = ServerProtectionCache.getFlagState(level.dimension().location(), pos, player, "block_interaction");

        if (ProtectionMixinUtils.isZoneActive(level, pos, fd) && ProtectionMixinUtils.isDenied(fd)) {
            ProtectionMixinUtils.message(player, fd);
            ProtectionMixinUtils.damage(player, fd);
            return InteractionResult.PASS;
        }

        return state.use(level, player, hand, hitResult);
    }
}
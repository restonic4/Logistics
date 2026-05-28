package com.restonic4.logistics.blocks.protector;

import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class ProtectionMixinUtils {
    private ProtectionMixinUtils() {}

    public static FlagData getFlag(Level level, BlockPos pos, Player player, String flagId) {
        if (level.isClientSide()) return null;
        return ServerProtectionCache.getFlagState(level.dimension().location(), pos, player, flagId);
    }

    public static FlagData getFlag(Player player, String flagId) {
        if (player.level().isClientSide()) return null;
        return ServerProtectionCache.getFlagState(
                player.level().dimension().location(),
                player.blockPosition(),
                player,
                flagId
        );
    }

    public static boolean isDenied(FlagData fd) {
        if (fd == null || !fd.enabled()) return false;
        try {
            ActionType action = ActionType.valueOf(fd.actionType());
            return action == ActionType.DENY || action == ActionType.MESSAGE || action == ActionType.DAMAGE;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void message(Player player, FlagData fd) {
        if (!fd.message().isEmpty() && player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(fd.message()));
        }
    }

    public static void damage(Player player, FlagData fd) {
        player.hurt(player.level().damageSources().generic(), (float) fd.damageValue());
    }

    /* ========== CallbackInfo ========== */

    public static boolean handle(Player player, FlagData fd, CallbackInfo ci) {
        if (!isDenied(fd)) return true;
        ActionType action = ActionType.valueOf(fd.actionType());
        switch (action) {
            case DENY -> ci.cancel();
            case MESSAGE -> { message(player, fd); ci.cancel(); }
            case DAMAGE -> { damage(player, fd); ci.cancel(); }
        }
        return false;
    }

    public static boolean handle(Player player, String flagId, CallbackInfo ci) {
        return handle(player, getFlag(player, flagId), ci);
    }

    public static boolean handle(Player player, BlockPos pos, String flagId, CallbackInfo ci) {
        return handle(player, getFlag(player.level(), pos, player, flagId), ci);
    }

    /* ========== CallbackInfoReturnable<Boolean> ========== */

    public static boolean handle(Player player, FlagData fd, CallbackInfoReturnable<Boolean> cir) {
        if (!isDenied(fd)) return true;
        ActionType action = ActionType.valueOf(fd.actionType());
        switch (action) {
            case DENY -> cir.setReturnValue(false);
            case MESSAGE -> { message(player, fd); cir.setReturnValue(false); }
            case DAMAGE -> { damage(player, fd); cir.setReturnValue(false); }
        }
        return false;
    }

    public static boolean handle(Player player, String flagId, CallbackInfoReturnable<Boolean> cir) {
        return handle(player, getFlag(player, flagId), cir);
    }

    public static boolean handle(Player player, BlockPos pos, String flagId, CallbackInfoReturnable<Boolean> cir) {
        return handle(player, getFlag(player.level(), pos, player, flagId), cir);
    }

    /* ========== CallbackInfoReturnable<<InteractionResult> ========== */

    public static boolean handleResult(Player player, FlagData fd, CallbackInfoReturnable<InteractionResult> cir) {
        if (!isDenied(fd)) return true;
        ActionType action = ActionType.valueOf(fd.actionType());
        switch (action) {
            case DENY -> cir.setReturnValue(InteractionResult.FAIL);
            case MESSAGE -> { message(player, fd); cir.setReturnValue(InteractionResult.FAIL); }
            case DAMAGE -> { damage(player, fd); cir.setReturnValue(InteractionResult.FAIL); }
        }
        return false;
    }

    public static boolean handleResult(Player player, String flagId, CallbackInfoReturnable<InteractionResult> cir) {
        return handleResult(player, getFlag(player, flagId), cir);
    }

    public static boolean handleResult(Player player, BlockPos pos, String flagId, CallbackInfoReturnable<InteractionResult> cir) {
        return handleResult(player, getFlag(player.level(), pos, player, flagId), cir);
    }

    /* ========== CallbackInfoReturnable<ItemStack> (Chorus Fruit) ========== */

    public static boolean handleStack(Player player, FlagData fd, CallbackInfoReturnable<ItemStack> cir, ItemStack returnValue) {
        if (!isDenied(fd)) return true;
        ActionType action = ActionType.valueOf(fd.actionType());
        switch (action) {
            case DENY -> cir.setReturnValue(returnValue);
            case MESSAGE -> { message(player, fd); cir.setReturnValue(returnValue); }
            case DAMAGE -> { damage(player, fd); cir.setReturnValue(returnValue); }
        }
        return false;
    }

    /* ========== CallbackInfoReturnable<ItemEntity> (Item Drop) ========== */

    public static boolean handleItemEntity(Player player, FlagData fd, CallbackInfoReturnable<ItemEntity> cir) {
        if (!isDenied(fd)) return true;
        ActionType action = ActionType.valueOf(fd.actionType());
        switch (action) {
            case DENY -> cir.setReturnValue(null);
            case MESSAGE -> { message(player, fd); cir.setReturnValue(null); }
            case DAMAGE -> { damage(player, fd); cir.setReturnValue(null); }
        }
        return false;
    }
}
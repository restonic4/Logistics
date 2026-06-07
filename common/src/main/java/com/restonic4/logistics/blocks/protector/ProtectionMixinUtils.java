package com.restonic4.logistics.blocks.protector;

import com.restonic4.logistics.blocks.protector.data_types.*;
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

    /** Queries the correct cache depending on logical side. */
    public static FlagData getFlag(Level level, BlockPos pos, Player player, String flagId) {
        if (level.isClientSide()) {
            return ClientProtectionCache.getFlagState(level.dimension().location(), pos, player, flagId);
        }
        return ServerProtectionCache.getFlagState(level.dimension().location(), pos, player, flagId);
    }

    /** Server-only helper for mixins living on server classes. */
    public static FlagData getServerFlag(Level level, BlockPos pos, Player player, String flagId) {
        if (level.isClientSide()) return null;
        return ServerProtectionCache.getFlagState(level.dimension().location(), pos, player, flagId);
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

    /** True when a zone exists and the flag is explicitly disabled (allowed). Used for exception logic. */
    public static boolean isExplicitlyAllowed(FlagData fd) {
        return fd != null && !fd.enabled();
    }

    /** True when a zone exists and the flag is enabled (denied). */
    public static boolean isZoneActive(Level level, BlockPos pos, FlagData fd) {
        return (fd != null && fd.enabled()) && hasPower(level, pos);
    }

    public static boolean hasPower(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return ClientProtectionCache.hasPower(level.dimension().location(), pos);
        }
        return ServerProtectionCache.hasPower(level.dimension().location(), pos);
    }

    public static ActionType getActionType(FlagData fd, ActionType defaultAction) {
        try {
            return ActionType.valueOf(fd.actionType());
        } catch (IllegalArgumentException e) {
            return defaultAction;
        }
    }

    public static ActionType getActionType(FlagData fd) {
        return getActionType(fd, ActionType.DENY);
    }

    public static void message(Player player, FlagData fd) {
        if (!fd.message().isEmpty() && player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(fd.message()));
        }
    }

    public static void damage(Player player, FlagData fd) {
        player.hurt(player.level().damageSources().generic(), (float) fd.damageValue());
    }

    /* ========== Generic Return ========== */

    public static <T> boolean handleReturn(Player player, FlagData fd, CallbackInfoReturnable<T> cir, T failValue) {
        if (!isDenied(fd)) return true;
        ActionType action = ActionType.valueOf(fd.actionType());
        switch (action) {
            case DENY -> cir.setReturnValue(failValue);
            case MESSAGE -> { message(player, fd); cir.setReturnValue(failValue); }
            case DAMAGE -> { damage(player, fd); cir.setReturnValue(failValue); }
        }
        return false;
    }

    /* ========== void ========== */

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
        return handle(player, getServerFlag(player.level(), player.blockPosition(), player, flagId), ci);
    }

    public static boolean handle(Player player, BlockPos pos, String flagId, CallbackInfo ci) {
        return handle(player, getServerFlag(player.level(), pos, player, flagId), ci);
    }

    /* ========== Boolean ========== */

    public static boolean handle(Player player, FlagData fd, CallbackInfoReturnable<Boolean> cir) {
        return handleReturn(player, fd, cir, false);
    }

    public static boolean handle(Player player, String flagId, CallbackInfoReturnable<Boolean> cir) {
        return handle(player, getServerFlag(player.level(), player.blockPosition(), player, flagId), cir);
    }

    public static boolean handle(Player player, BlockPos pos, String flagId, CallbackInfoReturnable<Boolean> cir) {
        return handle(player, getServerFlag(player.level(), pos, player, flagId), cir);
    }

    /* ========== InteractionResult ========== */

    public static boolean handleResult(Player player, FlagData fd, CallbackInfoReturnable<InteractionResult> cir) {
        return handleReturn(player, fd, cir, InteractionResult.FAIL);
    }

    public static boolean handleResult(Player player, String flagId, CallbackInfoReturnable<InteractionResult> cir) {
        return handleResult(player, getServerFlag(player.level(), player.blockPosition(), player, flagId), cir);
    }

    public static boolean handleResult(Player player, BlockPos pos, String flagId, CallbackInfoReturnable<InteractionResult> cir) {
        return handleResult(player, getServerFlag(player.level(), pos, player, flagId), cir);
    }

    /* ========== Legacy wrappers (kept for compat) ========== */

    public static boolean handleStack(Player player, FlagData fd, CallbackInfoReturnable<ItemStack> cir, ItemStack returnValue) {
        return handleReturn(player, fd, cir, returnValue);
    }

    public static boolean handleItemEntity(Player player, FlagData fd, CallbackInfoReturnable<ItemEntity> cir) {
        return handleReturn(player, fd, cir, null);
    }
}
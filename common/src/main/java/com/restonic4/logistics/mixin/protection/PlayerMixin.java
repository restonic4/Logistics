package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.ClientProtectionCache;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {

    // ==================== PvP ====================
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide()) return;

        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player attackerPlayer) || attackerPlayer == self) return;

        FlagData fd = ServerProtectionCache.getFlagState(
                attackerPlayer.level().dimension().location(),
                attackerPlayer.blockPosition(),
                attackerPlayer,
                "pvp"
        );
        if (fd == null || !fd.enabled()) return;

        ActionType action;
        try { action = ActionType.valueOf(fd.actionType()); } catch (IllegalArgumentException e) { return; }

        switch (action) {
            case DENY -> cir.setReturnValue(false);
            case MESSAGE -> {
                if (!fd.message().isEmpty() && attackerPlayer instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal(fd.message()));
                }
                cir.setReturnValue(false);
            }
            case DAMAGE -> {
                attackerPlayer.hurt(self.level().damageSources().generic(), (float) fd.damageValue());
                cir.setReturnValue(false);
            }
        }
    }

    // ==================== entity_interaction + villager_trade exception ====================
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void onInteractOn(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide() || !(self instanceof ServerPlayer player)) return;

        if (entity instanceof AbstractVillager) {
            FlagData villagerFd = ServerProtectionCache.getFlagState(
                    player.level().dimension().location(), player.blockPosition(), player, "villager_trade");
            if (villagerFd != null && !villagerFd.enabled()) return; // explicitly allowed
            if (villagerFd != null && villagerFd.enabled()) {
                ProtectionMixinUtils.handleResult(player, villagerFd, cir);
                return;
            }
        }

        FlagData fd = ServerProtectionCache.getFlagState(
                player.level().dimension().location(), player.blockPosition(), player, "entity_interaction");
        ProtectionMixinUtils.handleResult(player, fd, cir);
    }

    // ==================== attack_entities ====================
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide() || !(self instanceof ServerPlayer player)) return;
        if (target instanceof Player) return; // PvP handled above

        FlagData fd = ServerProtectionCache.getFlagState(
                player.level().dimension().location(), player.blockPosition(), player, "attack_entities");
        ProtectionMixinUtils.handle(player, fd, ci);
    }

    // ==================== sneaking (tick) and walk_in ====================
    @Inject(method = "tick", at = @At("TAIL"))
    private void logistics$onTickTail(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) return;

        BlockPos pos = player.blockPosition();

        // ==================== SNEAKING SYSTEM ====================
        FlagData sneakingFlag = ProtectionMixinUtils.getServerFlag(player.level(), pos, player, "sneaking");
        if (ProtectionMixinUtils.isZoneDenied(sneakingFlag)) {
            ActionType sneakingAction = ProtectionMixinUtils.getActionType(sneakingFlag);

            switch (sneakingAction) {
                case DENY -> {
                    if (player.getPose() == Pose.CROUCHING && player.canEnterPose(Pose.STANDING)) {
                        player.setPose(Pose.STANDING);
                        player.refreshDimensions();
                    }
                    player.setShiftKeyDown(false);
                }
                case MESSAGE -> {
                    if (player.isShiftKeyDown() && player.canEnterPose(Pose.STANDING) && player.tickCount % 20 == 0) {
                        ProtectionMixinUtils.message(player, sneakingFlag);
                    }
                }
                case DAMAGE -> {
                    if (player.isShiftKeyDown() && player.canEnterPose(Pose.STANDING) && player.tickCount % 20 == 0) {
                        ProtectionMixinUtils.damage(player, sneakingFlag);
                    }
                }
            }
        }

        // ==================== WALK_IN SYSTEM ====================
        if (player.tickCount % 20 == 0) {
            FlagData walkInFlag = ProtectionMixinUtils.getServerFlag(player.level(), pos, player, "walk_in");

            if (ProtectionMixinUtils.isZoneDenied(walkInFlag)) {
                ActionType walkInAction = ProtectionMixinUtils.getActionType(walkInFlag);

                switch (walkInAction) {
                    case DAMAGE -> ProtectionMixinUtils.damage(player, walkInFlag);
                    case MESSAGE -> ProtectionMixinUtils.message(player, walkInFlag);
                }
            }
        }
    }

    @Inject(
            method = "maybeBackOffFromEdge",
            at = @At("HEAD"),
            cancellable = true
    )
    private void logistics$cancelEdgeBackOff(Vec3 movement, MoverType mover, CallbackInfoReturnable<Vec3> cir) {
        Player player = (Player)(Object)this;

        // Use the correct cache for the logical side
        FlagData fd = ProtectionMixinUtils.getFlag(player.level(), player.blockPosition(), player, "sneaking");
        if (!ProtectionMixinUtils.isZoneDenied(fd)) return;

        try {
            if (ActionType.valueOf(fd.actionType()) == ActionType.DENY) {
                // Return the original, unmodified movement vector — do not back off from the edge
                cir.setReturnValue(movement);
            }
        } catch (IllegalArgumentException ignored) {}
    }

    @Inject(method = "getMovementEmission", at = @At("HEAD"), cancellable = true)
    private void logistics$forceMovementEmission(CallbackInfoReturnable<Entity.MovementEmission> cir) {
        Player player = (Player)(Object)this;

        FlagData fd = ProtectionMixinUtils.getFlag(player.level(), player.blockPosition(), player, "sneaking");
        if (!ProtectionMixinUtils.isZoneDenied(fd)) return;

        try {
            if (ActionType.valueOf(fd.actionType()) == ActionType.DENY) {
                cir.setReturnValue(Entity.MovementEmission.ALL);
            }
        } catch (IllegalArgumentException ignored) {}
    }
}
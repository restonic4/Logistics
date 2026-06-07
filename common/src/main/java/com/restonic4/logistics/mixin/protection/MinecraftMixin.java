package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.ClientProtectionCache;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void onStartUseItem(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        LocalPlayer player = mc.player;
        if (player == null || !player.level().isClientSide()) return;

        if (mc.hitResult instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();

            if (entity instanceof AbstractVillager) {
                FlagData fd = ClientProtectionCache.getFlagState(player.level().dimension().location(), player.blockPosition(), player, "villager_trade");
                if (ProtectionMixinUtils.isZoneActive(player.level(), player.blockPosition(), fd)) {
                    ci.cancel();
                }
                return;
            }

            FlagData fd = ClientProtectionCache.getFlagState(player.level().dimension().location(), player.blockPosition(), player, "entity_interaction");
            if (!ProtectionMixinUtils.isZoneActive(player.level(), player.blockPosition(), fd)) return;
            ci.cancel();
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        LocalPlayer player = mc.player;
        if (player == null || !player.level().isClientSide()) return;

        if (mc.hitResult instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (entity instanceof Player) return; // PvP handled elsewhere

            FlagData fd = ClientProtectionCache.getFlagState(player.level().dimension().location(), player.blockPosition(), player, "attack_entities");
            if(!ProtectionMixinUtils.isZoneActive(player.level(), player.blockPosition(), fd)) return;
            cir.setReturnValue(false);
        }
    }
}
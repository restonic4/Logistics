package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void onDropItem(ItemStack stack, boolean dropAround, boolean includeThrowerName, CallbackInfoReturnable<ItemEntity> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.level().isClientSide()) return;

        FlagData fd = ServerProtectionCache.getFlagState(
                self.level().dimension().location(), self.blockPosition(), self, "item_drop");
        ProtectionMixinUtils.handleItemEntity(self, fd, cir);
    }

    @Inject(method = "take", at = @At("HEAD"), cancellable = true)
    private void onTakeItem(Entity entity, int amount, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.level().isClientSide()) return;

        FlagData fd = ServerProtectionCache.getFlagState(
                self.level().dimension().location(), self.blockPosition(), self, "item_pickup");
        ProtectionMixinUtils.handle(self, fd, ci);
    }
}
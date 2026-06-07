package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.ClientProtectionCache;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void onClicked(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        boolean isThrow = clickType == ClickType.THROW;
        boolean isCursorDrop = clickType == ClickType.PICKUP && slotId == AbstractContainerMenu.SLOT_CLICKED_OUTSIDE;
        if (!isThrow && !isCursorDrop) return;

        if (player instanceof ServerPlayer serverPlayer) {
            FlagData fd = ServerProtectionCache.getFlagState(serverPlayer.level().dimension().location(), serverPlayer.blockPosition(), serverPlayer, "item_drop");
            if (ProtectionMixinUtils.isZoneActive(serverPlayer.level(), serverPlayer.blockPosition(), fd)) {
                ci.cancel();
            }
        } else {
            FlagData fd = ClientProtectionCache.getFlagState(player.level().dimension().location(), player.blockPosition(), player, "item_drop");
            if (ProtectionMixinUtils.isZoneActive(player.level(), player.blockPosition(), fd)) {
                ci.cancel();
            }
        }
    }
}
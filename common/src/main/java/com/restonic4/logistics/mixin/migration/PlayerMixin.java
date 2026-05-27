package com.restonic4.logistics.mixin.migration;

import com.restonic4.logistics.migration.MigrationManager;
import com.restonic4.logistics.migration.NbtWalker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void onPlayerLoad(CompoundTag tag, CallbackInfo ci) {
        if (tag != null) {
            NbtWalker.processContainer(tag);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onPlayerSave(CompoundTag tag, CallbackInfo ci) {
        if (tag != null) {
            tag.putInt(MigrationManager.VERSION_KEY, MigrationManager.CURRENT_DATA_VERSION);
        }
    }
}

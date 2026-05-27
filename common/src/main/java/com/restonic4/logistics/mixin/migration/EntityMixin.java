package com.restonic4.logistics.mixin.migration;

import com.restonic4.logistics.migration.MigrationManager;
import com.restonic4.logistics.migration.NbtWalker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "load", at = @At("HEAD"))
    private void onEntityLoad(CompoundTag compound, CallbackInfo ci) {
        if (compound != null) {
            NbtWalker.processContainer(compound);
        }
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void onEntitySave(CompoundTag compound, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag result = cir.getReturnValue();
        if (result != null) {
            result.putInt(MigrationManager.VERSION_KEY, MigrationManager.CURRENT_DATA_VERSION);
        }
    }
}
package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sheep.class)
public class SheepMixin {

    @Inject(method = "ate", at = @At("HEAD"), cancellable = true)
    private void onAte(CallbackInfo ci) {
        Sheep self = (Sheep) (Object) this;
        if (self.level().isClientSide()) return;

        BlockPos pos = self.blockPosition();
        FlagData fd = ServerProtectionCache.getFlagState(
                self.level().dimension().location(), pos, null, "mob_grief");
        if (fd == null || !fd.enabled()) return;

        try {
            ActionType action = ActionType.valueOf(fd.actionType());
            if (action == ActionType.DENY || action == ActionType.MESSAGE || action == ActionType.DAMAGE) ci.cancel();
        } catch (IllegalArgumentException ignored) {}
    }
}
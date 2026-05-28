package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {

    @Shadow @Final public Level level;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void onExplode(CallbackInfo ci) {
        if (this.level == null || this.level.isClientSide()) return;

        Vec3 pos = new Vec3(this.x, this.y, this.z);
        BlockPos blockPos = BlockPos.containing(pos);

        LivingEntity indirectSource = ((Explosion) (Object) this).getIndirectSourceEntity();
        Player player = indirectSource instanceof Player p ? p : null;

        FlagData fd = player != null
                ? ServerProtectionCache.getFlagState(this.level.dimension().location(), player.blockPosition(), player, "explosions")
                : ServerProtectionCache.getFlagState(this.level.dimension().location(), blockPos, null, "explosions");

        if (fd == null || !fd.enabled()) return;

        ActionType action;
        try { action = ActionType.valueOf(fd.actionType()); } catch (IllegalArgumentException e) { return; }

        switch (action) {
            case DENY -> ci.cancel();
            case MESSAGE -> {
                if (player instanceof ServerPlayer sp && !fd.message().isEmpty()) {
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(fd.message()));
                }
                ci.cancel();
            }
            case DAMAGE -> {
                if (player != null) player.hurt(this.level.damageSources().generic(), (float) fd.damageValue());
                ci.cancel();
            }
        }
    }
}
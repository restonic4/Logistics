package com.restonic4.logistics.mixin.protection;

import com.restonic4.logistics.blocks.protector.ProtectionMixinUtils;
import com.restonic4.logistics.blocks.protector.data_types.ActionType;
import com.restonic4.logistics.blocks.protector.data_types.FlagData;
import com.restonic4.logistics.blocks.protector.data_types.ServerProtectionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
public class EndermanTakeBlockMixin {

    @Unique private BlockPos logistics$lastPos;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean onRemoveBlock(Level level, BlockPos pos, boolean move) {
        this.logistics$lastPos = pos;
        if (logistics$isProtected(level, pos)) return false;
        return level.removeBlock(pos, move);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/EnderMan;setCarriedBlock(Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void onSetCarriedBlock(EnderMan enderman, BlockState state) {
        if (this.logistics$lastPos != null && logistics$isProtected(enderman.level(), this.logistics$lastPos)) {
            this.logistics$lastPos = null;
            return;
        }
        this.logistics$lastPos = null;
        enderman.setCarriedBlock(state);
    }

    @Unique
    private boolean logistics$isProtected(Level level, BlockPos pos) {
        if (level == null || level.isClientSide()) return false;
        FlagData fd = ServerProtectionCache.getFlagState(level.dimension().location(), pos, null, "mob_grief");
        if (!(ProtectionMixinUtils.isZoneActive(level, pos, fd))) return false;
        try {
            ActionType action = ActionType.valueOf(fd.actionType());
            return action == ActionType.DENY || action == ActionType.MESSAGE || action == ActionType.DAMAGE;
        } catch (IllegalArgumentException e) { return false; }
    }
}
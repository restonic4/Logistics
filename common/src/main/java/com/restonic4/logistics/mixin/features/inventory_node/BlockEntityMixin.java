package com.restonic4.logistics.mixin.features.inventory_node;

import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.nodes.InventoryNode;
import com.restonic4.logistics.utils.MinecraftUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
    @Shadow protected Level level;
    @Shadow public abstract BlockPos getBlockPos();

    @Inject(method = "setChanged*", at = @At("TAIL"))
    private void onSetChanged(CallbackInfo ci) {
        if (this.level == null) return;
        if (this.level.isClientSide()) return;
        if (!(this instanceof Container)) return;

        Optional<InventoryNode> inventoryNode = MinecraftUtils.findNeighbor(this.getBlockPos(), (blockPos) -> {
            NetworkNode node = NetworkManager.get((ServerLevel) this.level).getNodeByBlockPos(blockPos);
            if (node instanceof InventoryNode foundInventoryNode) return foundInventoryNode;
            return null;
        });

        inventoryNode.ifPresent(InventoryNode::onExternalContentChanged);
    }
}
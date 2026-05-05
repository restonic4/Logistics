package com.restonic4.logistics.networks.tooltip;

import com.restonic4.logistics.compatibility.CompatibilityManager;
import com.restonic4.logistics.compatibility.create.CreateCompatibility;
import com.restonic4.logistics.networking.NetworkTooltipPayload;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.UUID;

public class NetworkScannerServerHandler {
    private static final double REACH = 6.0;
    private static final boolean DEBUG = true;

    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            sendClear(player);
            return;
        }

        HitResult hit = player.pick(REACH, 1.0f, false);

        if (hit.getType() != HitResult.Type.BLOCK) {
            sendClear(player);
            return;
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        NetworkNode node = NetworkManager.get(serverLevel).getNodeByBlockPos(pos);
        if (node == null) {
            sendClear(player);
            return;
        }

        TooltipBuilder builder = new TooltipBuilder();
        boolean added = node.buildNetworkTooltip(builder, player.isShiftKeyDown(), DEBUG);

        if (!added) {
            sendClear(player);
            return;
        }

        boolean areCreateGogglesPresent = CompatibilityManager.isCreateLoaded() && CreateCompatibility.hasGoggleOverlay(serverLevel, pos);
        NetworkTooltipPayload.from(builder, areCreateGogglesPresent).sendTo(player);
    }

    private static void sendClear(ServerPlayer player) {
        NetworkTooltipPayload.from(new TooltipBuilder(), false).sendTo(player);
    }
}

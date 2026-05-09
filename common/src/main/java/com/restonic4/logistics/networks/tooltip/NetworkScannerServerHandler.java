package com.restonic4.logistics.networks.tooltip;

import com.restonic4.logistics.compatibility.CompatibilityManager;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

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

        Network network = node.getNetwork();
        if (network == null) {
            sendClear(player);
            return;
        }

        TooltipBuilder builder = new TooltipBuilder();
        boolean added = network.buildScannerTooltip(builder, player.isShiftKeyDown());
        added = node.buildScannerTooltip(builder, player.isShiftKeyDown()) || added;
        if (added) builder.mainTitle("Scanner", ChatFormatting.GOLD);

        if (DEBUG) {
            if (added) {
                builder.spacer();
            }
            added = network.buildDebugScannerTooltip(builder, player.isShiftKeyDown()) || added;
            added = node.buildDebugScannerTooltip(builder, player.isShiftKeyDown()) || added;
        }

        if (!added) {
            sendClear(player);
            return;
        }

        boolean areCreateGogglesPresent = CompatibilityManager.isCreateLoaded() && CompatibilityManager.getCreateCompatibilityLayer().hasGoggleOverlay(serverLevel, pos);
        ServerNetworking.sendToClient(player, NetworkTooltipPacket.from(builder, areCreateGogglesPresent));
    }

    private static void sendClear(ServerPlayer player) {
        ServerNetworking.sendToClient(player, NetworkTooltipPacket.from(new TooltipBuilder(), false));
    }
}

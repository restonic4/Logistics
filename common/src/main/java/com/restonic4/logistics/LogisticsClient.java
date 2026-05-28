package com.restonic4.logistics;

import com.restonic4.logistics.blocks.ClientBlockRegistry;
import com.restonic4.logistics.blocks.charging_station.ChargingStationSyncPacket;
import com.restonic4.logistics.blocks.computer.ComputerLogPushPacket;
import com.restonic4.logistics.blocks.computer.ComputerLogSyncPacket;
import com.restonic4.logistics.blocks.computer.ComputerOffPacket;
import com.restonic4.logistics.blocks.computer.ComputerSyncPacket;
import com.restonic4.logistics.blocks.computer.protection.ProtectionCacheSyncPacket;
import com.restonic4.logistics.blocks.computer.protection.ProtectionEditSyncPacket;
import com.restonic4.logistics.compatibility.CompatibilityManager;
import com.restonic4.logistics.events.RenderCallbacks;
import com.restonic4.logistics.experiment.ShockwavePacket;
import com.restonic4.logistics.networking.NetworkingRegistry;
import com.restonic4.logistics.networks.pathfinding.ParcelRenderSyncPacket;
import com.restonic4.logistics.networks.tooltip.NetworkTooltipPacket;
import com.restonic4.logistics.rendering.NetworkDebugRenderer;
import com.restonic4.logistics.rendering.ParcelRenderer;
import com.restonic4.logistics.screens.NetworkScannerOverlay;

public class LogisticsClient {
    public static void init() {
        ClientBlockRegistry.register();
        CompatibilityManager.registerClient();
        NetworkScannerOverlay.register();

        RenderCallbacks.ON_LEVEL_RENDERED.register(NetworkDebugRenderer::render);
        ParcelRenderer.register();

        NetworkingRegistry.registerClientTargetedPacket(NetworkTooltipPacket.ID, NetworkTooltipPacket::new);
        NetworkingRegistry.registerClientTargetedPacket(ComputerSyncPacket.ID, ComputerSyncPacket::new);
        NetworkingRegistry.registerClientTargetedPacket(ComputerLogSyncPacket.ID, ComputerLogSyncPacket::new);
        NetworkingRegistry.registerClientTargetedPacket(ComputerLogPushPacket.ID, ComputerLogPushPacket::new);
        NetworkingRegistry.registerClientTargetedPacket(ComputerOffPacket.ID, ComputerOffPacket::new);
        NetworkingRegistry.registerClientTargetedPacket(ParcelRenderSyncPacket.ID, ParcelRenderSyncPacket::new);
        NetworkingRegistry.registerClientTargetedPacket(ShockwavePacket.ID, ShockwavePacket::new);
        NetworkingRegistry.registerClientTargetedPacket(ChargingStationSyncPacket.ID, ChargingStationSyncPacket::new);
        NetworkingRegistry.registerClientTargetedPacket(ProtectionEditSyncPacket.ID, ProtectionEditSyncPacket::read);
        NetworkingRegistry.registerClientTargetedPacket(ProtectionCacheSyncPacket.ID, ProtectionCacheSyncPacket::read);
    }
}

package com.restonic4.logistics;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.ClientBlockRegistry;
import com.restonic4.logistics.blocks.computer.ComputerSyncPacket;
import com.restonic4.logistics.compatibility.CompatibilityManager;
import com.restonic4.logistics.events.RenderCallbacks;
import com.restonic4.logistics.networking.NetworkingRegistry;
import com.restonic4.logistics.networks.pathfinding.ParcelRenderSyncPacket;
import com.restonic4.logistics.networks.tooltip.NetworkTooltipPacket;
import com.restonic4.logistics.registry.builders.ClientBlockBuilder;
import com.restonic4.logistics.rendering.NetworkDebugRenderer;
import com.restonic4.logistics.rendering.ParcelRenderer;
import com.restonic4.logistics.screens.NetworkScannerOverlay;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

public class LogisticsClient {
    public static void init() {
        ClientBlockRegistry.register();
        CompatibilityManager.registerClient();
        NetworkScannerOverlay.register();

        RenderCallbacks.ON_LEVEL_RENDERED.register(NetworkDebugRenderer::render);
        ParcelRenderer.register();

        NetworkingRegistry.registerClientTargetedPacket(NetworkTooltipPacket.ID, NetworkTooltipPacket::new);
        NetworkingRegistry.registerClientTargetedPacket(ComputerSyncPacket.ID, ComputerSyncPacket::new);
        NetworkingRegistry.registerClientTargetedPacket(ParcelRenderSyncPacket.ID, ParcelRenderSyncPacket::new);
    }
}

package com.restonic4.logistics;

import com.restonic4.logistics.audio.ServerAudioStorage;
import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.charging_station.ChargingStationBlockEntity;
import com.restonic4.logistics.blocks.charging_station.ChargingStationRenderer;
import com.restonic4.logistics.experiment.DebugCommand;
import com.restonic4.logistics.experiment.DumpCommand;
import com.restonic4.logistics.experiment.TestScreenCommand;
import com.restonic4.logistics.networks.client.ClientNetworkManager;
import com.restonic4.logistics.registry.ClientBlockRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class LogisticsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LogisticsClient.init();
        ClientBlockRegistry.apply();
        TestScreenCommand.init();
        DebugCommand.init();
        DumpCommand.init();

        BlockRenderLayerMap.INSTANCE.putBlock(
                BlockRegistry.PROTECTOR_BLOCK.getBlock(),
                RenderType.translucent()
        );

        BlockRenderLayerMap.INSTANCE.putBlock(
                BlockRegistry.CREATIVE_PROTECTOR_BLOCK.getBlock(),
                RenderType.translucent()
        );

        BlockEntityRenderers.register(BlockRegistry.CHARGING_STATION_BLOCK.getBlockEntityType(ChargingStationBlockEntity.class), ChargingStationRenderer::new);

        ClientPlayConnectionEvents.DISCONNECT.register((packetListener, minecraft) -> {
            ClientNetworkManager.clear();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
            ServerAudioStorage.clear();
        });
    }
}

package com.restonic4.logistics;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.charging_station.ChargingStationBlockEntity;
import com.restonic4.logistics.blocks.charging_station.ChargingStationRenderer;
import com.restonic4.logistics.experiment.DebugCommand;
import com.restonic4.logistics.experiment.TestScreenCommand;
import com.restonic4.logistics.registry.ClientBlockRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class LogisticsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LogisticsClient.init();
        ClientBlockRegistry.apply();
        TestScreenCommand.init();
        DebugCommand.init();

        BlockRenderLayerMap.INSTANCE.putBlock(
                BlockRegistry.PROTECTOR_BLOCK.getBlock(),
                RenderType.translucent()
        );

        BlockRenderLayerMap.INSTANCE.putBlock(
                BlockRegistry.CREATIVE_PROTECTOR_BLOCK.getBlock(),
                RenderType.translucent()
        );

        BlockEntityRenderers.register(BlockRegistry.CHARGING_STATION_BLOCK.getBlockEntityType(ChargingStationBlockEntity.class), ChargingStationRenderer::new);
    }
}

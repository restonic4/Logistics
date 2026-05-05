package com.restonic4.logistics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class LogisticsForge {
    public LogisticsForge(FMLJavaModLoadingContext context) {
        Logistics.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.getModEventBus().addListener(this::onClientSetup);
        }
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        LogisticsClient.init();
    }
}
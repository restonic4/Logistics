package com.restonic4.logistics;

import com.restonic4.logistics.platform.ForgeRegistry;
import com.restonic4.logistics.platform.Services;
import com.restonic4.logistics.registry.ClientBlockRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class LogisticsForge {
    public LogisticsForge(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        ((ForgeRegistry<?,?,?,?>) Services.PLATFORM_REGISTRY).init(modBus);

        Logistics.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(this::onClientSetup);
        }
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        LogisticsClient.init();
        ClientBlockRegistry.apply();
    }
}
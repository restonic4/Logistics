package com.restonic4.logistics;

import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;

@Mod(Constants.MOD_ID)
public class LogisticsForge {
    public LogisticsForge(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(this::onRegister);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.getModEventBus().addListener(this::onClientSetup);
        }
    }

    private void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            Logistics.init();
        }
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        LogisticsClient.init();
    }
}
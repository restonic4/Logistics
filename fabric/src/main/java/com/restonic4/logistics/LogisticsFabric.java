package com.restonic4.logistics;

import com.restonic4.logistics.registry.PlatformRegistry;
import net.fabricmc.api.ModInitializer;

public class LogisticsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Logistics.init();
        PlatformRegistry.freeze();
    }
}
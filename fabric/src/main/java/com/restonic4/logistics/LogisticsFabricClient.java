package com.restonic4.logistics;

import com.restonic4.logistics.registry.ClientBlockRegistry;
import net.fabricmc.api.ClientModInitializer;

public class LogisticsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LogisticsClient.init();
        ClientBlockRegistry.apply();
    }
}

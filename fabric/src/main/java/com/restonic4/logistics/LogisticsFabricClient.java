package com.restonic4.logistics;

import net.fabricmc.api.ClientModInitializer;

public class LogisticsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LogisticsClient.init();
    }
}

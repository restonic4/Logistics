package com.restonic4.logistics;

import net.fabricmc.api.ModInitializer;

public class LogisticsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Logistics.init();
    }
}
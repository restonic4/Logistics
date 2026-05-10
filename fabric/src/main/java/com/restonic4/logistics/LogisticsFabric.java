package com.restonic4.logistics;

import com.restonic4.logistics.events.ChunkEvents;
import com.restonic4.logistics.registry.PlatformRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;

public class LogisticsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Logistics.init();
        PlatformRegistry.freeze();
    }
}
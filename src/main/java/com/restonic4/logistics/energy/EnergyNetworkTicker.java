package com.restonic4.logistics.energy;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class EnergyNetworkTicker {
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(EnergyNetworkTicker::onServerTick);
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> EnergyNetworkManager.get(level).onChunkLoaded(level, chunk.getBlockEntities().values()));
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> EnergyNetworkManager.get(level).onChunkUnloaded(chunk.getBlockEntities().values()));
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            EnergyNetworkManager manager = EnergyNetworkManager.get(level);
            manager.tick(level.getGameTime());
        }
    }
}
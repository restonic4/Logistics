package com.restonic4.logistics;

import com.restonic4.logistics.blocks.BlockEntityRegistry;
import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.commands.NetworkDebugCommand;
import com.restonic4.logistics.energy.EnergyNetworkManager;
import com.restonic4.logistics.energy.EnergyNetworkTicker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.resources.ResourceLocation;

public class Logistics implements ModInitializer {
    @Override
    public void onInitialize() {
        BlockRegistry.register();
        BlockEntityRegistry.register();

        EnergyNetworkTicker.register();

        //CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> NetworkDebugCommand.register(dispatcher));
    }

    public static ResourceLocation id(String id) {
        return new ResourceLocation(Constants.MOD_ID, id);
    }
}

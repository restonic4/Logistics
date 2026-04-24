package com.restonic4.logistics;

import com.restonic4.logistics.blocks.BlockRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

public class Logistics implements ModInitializer {
    @Override
    public void onInitialize() {
        BlockRegistry.register();
    }

    public static ResourceLocation id(String id) {
        return new ResourceLocation(Constants.MOD_ID, id);
    }
}

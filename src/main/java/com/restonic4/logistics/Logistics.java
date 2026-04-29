package com.restonic4.logistics;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.energy.NetworkManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class Logistics implements ModInitializer {
    public static final CreativeModeTab CUSTOM_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            id("custom_tab"),
            FabricItemGroup.builder()
                    .title(Component.translatable("itemGroup." + Constants.MOD_ID + ".custom_tab"))
                    .icon(() -> new ItemStack(BlockRegistry.BATTERY_BLOCK.getItem()))
                    .displayItems((parameters, output) -> {
                        output.accept(BlockRegistry.BASIC_PIPE.getItem());
                        output.accept(BlockRegistry.BATTERY_BLOCK.getItem());
                        output.accept(BlockRegistry.GENERATOR_BLOCK.getItem());
                        output.accept(BlockRegistry.MACHINE_BLOCK.getItem());
                    })
                    .build()
    );

    @Override
    public void onInitialize() {
        BlockRegistry.register();
        NetworkManager.register();
    }

    public static ResourceLocation id(String id) {
        return new ResourceLocation(Constants.MOD_ID, id);
    }
}

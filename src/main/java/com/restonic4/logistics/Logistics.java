package com.restonic4.logistics;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.compatibility.CompatibilityManager;
import com.restonic4.logistics.networking.NetworkTooltipPayload;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.tooltip.NetworkScannerServerHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class Logistics implements ModInitializer {
    public static final ResourceKey<CreativeModeTab> CUSTOM_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            id("custom_tab")
    );
    public static final CreativeModeTab CUSTOM_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            CUSTOM_TAB_KEY,
            FabricItemGroup.builder()
                    .title(Component.translatable("itemGroup." + Constants.MOD_ID + ".custom_tab"))
                    .icon(() -> new ItemStack(BlockRegistry.BATTERY_BLOCK.getItem()))
                    .build()
    );

    @Override
    public void onInitialize() {
        BlockRegistry.register();
        CompatibilityManager.register();
        NetworkManager.register();
        NetworkTooltipPayload.register();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                NetworkScannerServerHandler.tick(p);
            }
        });
    }

    public static ResourceLocation id(String id) {
        return new ResourceLocation(Constants.MOD_ID, id);
    }
}

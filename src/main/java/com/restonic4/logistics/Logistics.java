package com.restonic4.logistics;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.compatibility.CompatibilityManager;
import com.restonic4.logistics.networks.NetworkManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
        // TODO: There is a massive bug here, so, if we register the network before compat blocks, it breaks the mod, like, in crazy ways that makes no sense at all. I should take a deep look at this, because wtf
        BlockRegistry.register();
        CompatibilityManager.register();
        NetworkManager.register();
    }

    public static ResourceLocation id(String id) {
        return new ResourceLocation(Constants.MOD_ID, id);
    }
}

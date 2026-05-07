package com.restonic4.logistics.registry;

import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.platform.Services;
import com.restonic4.logistics.registry.builders.BlockBuilder;
import com.restonic4.logistics.registry.builders.CreativeTabBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PlatformRegistry {
    private static final Map<ResourceKey<CreativeModeTab>, List<Supplier<Item>>> CREATIVE_TAB_INJECTIONS = new LinkedHashMap<>();

    public static <B extends Block, N extends NetworkNode> BlockBuilder<B, N> block(
            ResourceLocation resourceLocation, Supplier<B> blockFactory
    ) {
        return new BlockBuilder<>(resourceLocation, blockFactory);
    }

    public static CreativeTabBuilder tab(
            ResourceLocation resourceLocation, Supplier<ItemStack> iconSupplier
    ) {
        return new CreativeTabBuilder(resourceLocation, iconSupplier);
    }

    public static void scheduleCreativeTabInjection(ResourceKey<CreativeModeTab> tabKey, Supplier<Item> itemSupplier) {
        CREATIVE_TAB_INJECTIONS.computeIfAbsent(tabKey, k -> new ArrayList<>()).add(itemSupplier);
    }

    public static List<Supplier<Item>> getCreativeTabInjections(ResourceKey<CreativeModeTab> tabKey) {
        return CREATIVE_TAB_INJECTIONS.getOrDefault(tabKey, List.of());
    }

    public static void freeze() {
        Services.PLATFORM_REGISTRY.freeze();
    }
}

package com.restonic4.logistics.registry;

import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.platform.Services;
import com.restonic4.logistics.registry.builders.BlockBuilder;
import com.restonic4.logistics.registry.builders.CreativeTabBuilder;
import com.restonic4.logistics.registry.entries.SelfDropEntry;
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

    private static final Map<ResourceLocation, List<ResourceLocation>> BLOCK_TAG_INJECTIONS = new LinkedHashMap<>();
    private static boolean FROZEN_BLOCK_TAG_INJECTIONS = false;

    private static final List<SelfDropEntry> SELF_DROP_LOOT_INJECTIONS = new ArrayList<>();
    private static boolean FROZEN_SELF_DROP_LOOT_INJECTIONS = false;

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

    public static void scheduleBlockTagInjection(ResourceLocation tagId, ResourceLocation blockId) {
        if (FROZEN_BLOCK_TAG_INJECTIONS) throw new RuntimeException("Could not register tag injection, registry frozen!");
        BLOCK_TAG_INJECTIONS.computeIfAbsent(tagId, k -> new ArrayList<>()).add(blockId);
    }

    public static Map<ResourceLocation, List<ResourceLocation>> getAndFreezeBlockTagInjections() {
        FROZEN_BLOCK_TAG_INJECTIONS = true;
        return BLOCK_TAG_INJECTIONS;
    }

    public static void scheduleSelfDropLootInjection(ResourceLocation blockId, boolean survivesExplosion) {
        if (FROZEN_SELF_DROP_LOOT_INJECTIONS) throw new RuntimeException("Could not register loot injection, registry frozen!");
        SELF_DROP_LOOT_INJECTIONS.add(new SelfDropEntry(blockId, survivesExplosion));
    }

    public static List<SelfDropEntry> getAndFreezeSelfDropLootInjections() {
        FROZEN_SELF_DROP_LOOT_INJECTIONS = true;
        return SELF_DROP_LOOT_INJECTIONS;
    }

    public static void freeze() {
        Services.PLATFORM_REGISTRY.freeze();
    }
}

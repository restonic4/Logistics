package com.restonic4.logistics.registry;

import com.restonic4.logistics.events.ServerTickEvents;
import com.restonic4.logistics.events.core.Event;
import com.restonic4.logistics.events.core.EventFactory;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class CreativeTabRegistry {
    private static final List<TabEntry> PENDING_TABS = new ArrayList<>();
    private static final Map<ResourceKey<CreativeModeTab>, List<Supplier<Item>>> INJECTIONS = new LinkedHashMap<>();

    private static boolean built = false;

    private CreativeTabRegistry() {}

    static TabEntry enqueue(ResourceKey<CreativeModeTab> key, CreativeModeTab tab) {
        if (built) throw new RuntimeException("Creative tabs have been already built!");

        TabEntry entry = new TabEntry(key, tab);
        PENDING_TABS.add(entry);
        return entry;
    }

    public static List<TabEntry> getPendingTabs() {
        return PENDING_TABS;
    }

    public static void scheduleInjection(ResourceKey<CreativeModeTab> tabKey, Supplier<Item> itemSupplier) {
        if (built) throw new RuntimeException("Creative tabs have been already built!");
        INJECTIONS.computeIfAbsent(tabKey, k -> new ArrayList<>()).add(itemSupplier);
    }

    public static List<Supplier<Item>> getInjections(ResourceKey<CreativeModeTab> tabKey) {
        return INJECTIONS.getOrDefault(tabKey, List.of());
    }

    public static void build() {
        if (built) throw new RuntimeException("Creative tabs have been already built!");
        built = true;

        for (CreativeTabRegistry.TabEntry entry : CreativeTabRegistry.getPendingTabs()) {
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, entry.key().location(), entry.tab());
        }
    }

    public record TabEntry(ResourceKey<CreativeModeTab> key, CreativeModeTab tab) {}
}
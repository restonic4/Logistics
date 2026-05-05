package com.restonic4.logistics.registry;

import com.restonic4.logistics.events.ServerTickEvents;
import com.restonic4.logistics.events.core.Event;
import com.restonic4.logistics.events.core.EventFactory;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class CreativeTabRegistry {
    private static final List<CreativeTabEntry> PENDING_TABS = new ArrayList<>();
    private static final Map<ResourceKey<CreativeModeTab>, List<Supplier<Item>>> INJECTIONS = new LinkedHashMap<>();

    static void enqueue(CreativeTabEntry entry) {
        PENDING_TABS.add(entry);
    }

    public static void scheduleInjection(ResourceKey<CreativeModeTab> tabKey, Supplier<Item> itemSupplier) {
        INJECTIONS.computeIfAbsent(tabKey, k -> new ArrayList<>()).add(itemSupplier);
    }

    public static List<Supplier<Item>> getInjections(ResourceKey<CreativeModeTab> tabKey) {
        return INJECTIONS.getOrDefault(tabKey, List.of());
    }

    public static void build() {
        for (CreativeTabEntry entry : PENDING_TABS) {
            CreativeModeTab tab = CreativeModeTab.builder(entry.getRow(), entry.getColumn())
                    .title(entry.getTitle())
                    .icon(entry.getIconSupplier())
                    .displayItems((params, output) -> {
                        getInjections(entry.getKey()).forEach(itemSupplier -> {
                            output.accept(itemSupplier.get());
                        });
                    })
                    .build();

            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, entry.getKey().location(), tab);
            entry.setTab(tab);
        }
    }
}
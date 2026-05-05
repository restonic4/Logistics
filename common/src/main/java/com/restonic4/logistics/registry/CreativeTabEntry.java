package com.restonic4.logistics.registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

public final class CreativeTabEntry {

    private final ResourceKey<CreativeModeTab> key;
    private final CreativeModeTab tab;

    CreativeTabEntry(ResourceKey<CreativeModeTab> key, CreativeModeTab tab) {
        this.key = key;
        this.tab = tab;
    }

    public ResourceKey<CreativeModeTab> key() {
        return key;
    }

    public CreativeModeTab tab() {
        return tab;
    }
}
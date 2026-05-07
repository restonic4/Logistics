package com.restonic4.logistics.registry.builders;

import com.restonic4.logistics.platform.Services;
import com.restonic4.logistics.registry.entries.CreativeTabEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class CreativeTabBuilder {
    private final ResourceKey<CreativeModeTab> key;
    private final Supplier<ItemStack> iconSupplier;
    private Component title;
    private CreativeModeTab.Row row = CreativeModeTab.Row.TOP;
    private int column = 0;

    public CreativeTabBuilder(ResourceLocation id, Supplier<ItemStack> iconSupplier) {
        this.key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id);
        this.iconSupplier = iconSupplier;
        this.title = Component.translatable("itemGroup." + id.getNamespace() + "." + id.getPath());
    }

    public CreativeTabBuilder title(Component title) {
        this.title = title;
        return this;
    }

    public CreativeTabBuilder position(CreativeModeTab.Row row, int column) {
        this.row = row;
        this.column = column;
        return this;
    }

    public CreativeTabEntry register() {
        CreativeTabEntry entry = new CreativeTabEntry(key, iconSupplier, title, row, column);
        Services.PLATFORM_REGISTRY.fromCreativeTabBuilder(entry);
        return entry;
    }
}

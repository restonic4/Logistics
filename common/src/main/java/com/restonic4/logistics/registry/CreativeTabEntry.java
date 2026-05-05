package com.restonic4.logistics.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class CreativeTabEntry {
    private final ResourceKey<CreativeModeTab> key;
    @Nullable private CreativeModeTab tab;

    private final Supplier<ItemStack> iconSupplier;
    private Component title;
    private CreativeModeTab.Row row = CreativeModeTab.Row.TOP;
    private int column = 0;

    CreativeTabEntry(ResourceKey<CreativeModeTab> key, Supplier<ItemStack> iconSupplier, Component title, CreativeModeTab.Row row, int column) {
        this.key = key;
        this.iconSupplier = iconSupplier;
        this.title = title;
        this.row = row;
        this.column = column;
    }

    void setTab(@NotNull CreativeModeTab tab) {
        this.tab = tab;
    }

    public ResourceKey<CreativeModeTab> getKey() {
        return key;
    }

    public CreativeModeTab getTab() {
        if (tab == null) throw new IllegalStateException("CreativeTab '" + key.location() + "' accessed before build!");
        return tab;
    }

    public Supplier<ItemStack> getIconSupplier() {
        return iconSupplier;
    }

    public Component getTitle() {
        return title;
    }

    public CreativeModeTab.Row getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }
}
package com.restonic4.logistics.registry.entries;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class CreativeTabEntry {
    private boolean loaded = false;

    private final ResourceKey<CreativeModeTab> key;
    @Nullable private CreativeModeTab tab;

    private final Supplier<ItemStack> iconSupplier;
    private Component title;
    private CreativeModeTab.Row row;
    private int column;

    public CreativeTabEntry(ResourceKey<CreativeModeTab> key, Supplier<ItemStack> iconSupplier, Component title, CreativeModeTab.Row row, int column) {
        this.key = key;
        this.iconSupplier = iconSupplier;
        this.title = title;
        this.row = row;
        this.column = column;
    }

    private void assertLoaded() {
        if (!loaded) throw new IllegalStateException("CreativeTabEntry '" + key + "' accessed before platform registration completed. Are you accessing it too early?");
    }

    public void markLoaded(@NotNull CreativeModeTab tab) {
        this.tab = tab;
        this.loaded = true;
    }

    public ResourceKey<CreativeModeTab> getKey() {
        return key;
    }

    public CreativeModeTab getTab() {
        assertLoaded();
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

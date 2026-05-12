package com.restonic4.logistics.registry.entries;

import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class ItemEntry {
    private final ResourceLocation id;
    private boolean loaded = false;

    private Item item;

    public ItemEntry(ResourceLocation id) {
        this.id = id;
    }

    public void markLoaded(Item item) {
        this.item = item;
        this.loaded = true;
    }

    private void assertLoaded() {
        if (!loaded) throw new IllegalStateException("ItemEntry '" + id + "' accessed before platform registration completed. Are you accessing it too early?");
    }

    public ResourceLocation getId() { return id; }

    public Item getItem() {
        assertLoaded();
        return item;
    }
}
package com.restonic4.logistics.registry.builders;

import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.platform.Services;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import com.restonic4.logistics.registry.SoundRegistry;
import com.restonic4.logistics.registry.entries.BlockEntry;
import com.restonic4.logistics.registry.entries.ItemEntry;
import com.restonic4.logistics.registry.entries.SoundEventEntry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ItemBuilder {
    private final ResourceLocation id;
    private Supplier<Item> itemFactory;

    private final List<ResourceKey<CreativeModeTab>> tabs = new ArrayList<>();

    public ItemBuilder(ResourceLocation id, Supplier<Item> itemFactory) {
        this.id = id;
        this.itemFactory = itemFactory;
    }

    public final ItemBuilder addToTab(ResourceKey<CreativeModeTab> tabKey) {
        this.tabs.add(tabKey);
        return this;
    }

    public ItemEntry register() {
        return Services.PLATFORM_REGISTRY.fromItemBuilder(this.id, this.itemFactory, this.tabs);
    }
}
package com.restonic4.logistics.platform.services;

import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import com.restonic4.logistics.registry.entries.BlockEntry;
import com.restonic4.logistics.registry.entries.CreativeTabEntry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface TargetedPlatformRegistry<B extends Block, BE extends BlockEntity, I extends Item, N extends NetworkNode> {
    BlockEntry<B, N> fromBlockBuilder(
            ResourceLocation id,
            Supplier<? extends B> blockFactory,
            BlockEntityType.BlockEntitySupplier<BE> blockEntitySupplier,
            Function<B, Item> itemFactory,
            NodeTypeRegistry.NetworkNodeType<N> networkNodeType,
            List<ResourceKey<CreativeModeTab>> tab
    );

    void fromCreativeTabBuilder(CreativeTabEntry entry);

    void freeze();
}

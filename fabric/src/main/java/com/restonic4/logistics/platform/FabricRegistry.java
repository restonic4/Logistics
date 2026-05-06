package com.restonic4.logistics.platform;

import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.platform.services.TargetedPlatformRegistry;
import com.restonic4.logistics.registry.PlatformRegistry;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import com.restonic4.logistics.registry.entries.BlockEntry;
import com.restonic4.logistics.registry.entries.CreativeTabEntry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class FabricRegistry<B extends Block, BE extends BlockEntity, I extends Item, N extends NetworkNode> implements TargetedPlatformRegistry<B, BE, I, N> {
    private final List<Runnable> pendingMarks = new ArrayList<>();

    public BlockEntry<B, N> fromBlockBuilder(
            ResourceLocation id,
            Supplier<? extends B> blockFactory,
            BlockEntityType.BlockEntitySupplier<BE> blockEntitySupplier,
            Function<B, Item> itemFactory,
            NodeTypeRegistry.NetworkNodeType<N> networkNodeType,
            List<ResourceKey<CreativeModeTab>> tabs
    ) {
        // Block
        B block = blockFactory.get();
        if (block instanceof BaseNetworkBlock networkBlock && networkNodeType != null) {
            networkBlock.setNodeType(networkNodeType);
        }
        Registry.register(BuiltInRegistries.BLOCK, id, block);

        // Item
        Item item;
        if (itemFactory != null) {
            item = itemFactory.apply(block);
            Registry.register(BuiltInRegistries.ITEM, id, item);
        } else {
            item = null;
        }

        if (item != null) {
            for (ResourceKey<CreativeModeTab> tab: tabs) {
                PlatformRegistry.scheduleCreativeTabInjection(tab, () -> item);
            }
        }

        // BlockEntity
        BlockEntityType<BE> blockEntityType;
        if (blockEntitySupplier != null) {
            blockEntityType = BlockEntityType.Builder.of(blockEntitySupplier, block).build(null);
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, blockEntityType);
        } else {
            blockEntityType = null;
        }

        BlockEntry<B, N> entry = new BlockEntry<>(id);
        pendingMarks.add(() -> entry.markLoaded(
                () -> block,
                () -> blockEntityType,
                () -> item,
                networkNodeType
        ));
        return entry;
    }

    @Override
    public void fromCreativeTabBuilder(CreativeTabEntry entry) {
        CreativeModeTab tab = CreativeModeTab.builder(entry.getRow(), entry.getColumn())
                .title(entry.getTitle())
                .icon(entry.getIconSupplier())
                .displayItems((params, output) -> {
                    PlatformRegistry.getCreativeTabInjections(entry.getKey()).forEach(itemSupplier -> {
                        output.accept(itemSupplier.get());
                    });
                })
                .build();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, entry.getKey().location(), tab);
        entry.markLoaded(tab);
    }

    @Override
    public void freeze() {
        pendingMarks.forEach(Runnable::run);
        pendingMarks.clear();
    }
}

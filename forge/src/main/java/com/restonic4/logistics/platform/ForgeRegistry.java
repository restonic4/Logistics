package com.restonic4.logistics.platform;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.platform.services.TargetedPlatformRegistry;
import com.restonic4.logistics.registry.PlatformRegistry;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import com.restonic4.logistics.registry.entries.BlockEntry;
import com.restonic4.logistics.registry.entries.CreativeTabEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ForgeRegistry<B extends Block, BE extends BlockEntity, I extends Item, N extends NetworkNode> implements TargetedPlatformRegistry<B, BE, I, N> {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Constants.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Constants.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        CREATIVE_TABS.register(modBus);
    }

    public BlockEntry<B, N> fromBlockBuilder(
            ResourceLocation id,
            Supplier<? extends B> blockFactory,
            BlockEntityType.BlockEntitySupplier<BE> blockEntitySupplier,
            Function<B, Item> itemFactory,
            NodeTypeRegistry.NetworkNodeType<N> networkNodeType,
            List<ResourceKey<CreativeModeTab>> tabs
    ) {
        BlockEntry<B, N> entry = new BlockEntry<>(id);

        RegistryObject<B> blockHolder = BLOCKS.register(id.getPath(), () -> {
            B block = blockFactory.get();
            if (block instanceof BaseNetworkBlock networkBlock && networkNodeType != null) {
                networkBlock.setNodeType(networkNodeType);
            }
            return block;
        });

        RegistryObject<Item> itemHolder = null;
        if (itemFactory != null) {
            itemHolder = ITEMS.register(id.getPath(), () -> {
                Item item = itemFactory.apply(blockHolder.get());
                for (ResourceKey<CreativeModeTab> tab: tabs) {
                    PlatformRegistry.scheduleCreativeTabInjection(tab, () -> item);
                }
                return item;
            });
        }

        RegistryObject<BlockEntityType<BE>> blockEntityHolder = blockEntitySupplier != null
                ? BLOCK_ENTITIES.register(id.getPath(), () -> BlockEntityType.Builder.of(blockEntitySupplier, blockHolder.get()).build(null))
                : null;

        entry.markLoaded(
                blockHolder::get,
                blockEntityHolder != null ? blockEntityHolder::get : null,
                itemHolder != null ? itemHolder::get : null,
                networkNodeType
        );

        return entry;
    }

    @Override
    public void fromCreativeTabBuilder(CreativeTabEntry entry) {
        CREATIVE_TABS.register(entry.getKey().location().getPath(), () -> {
            CreativeModeTab tab = CreativeModeTab.builder(entry.getRow(), entry.getColumn())
                    .title(entry.getTitle())
                    .icon(entry.getIconSupplier())
                    .displayItems((params, output) -> {
                        PlatformRegistry.getCreativeTabInjections(entry.getKey()).forEach(itemSupplier -> {
                            output.accept(itemSupplier.get());
                        });
                    })
                    .build();
            entry.markLoaded(tab);
            return tab;
        });
    }

    @Override
    public void freeze() {
        // no-op
    }
}

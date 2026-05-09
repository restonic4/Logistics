package com.restonic4.logistics.registry;

import com.restonic4.logistics.LogisticsClient;
import com.restonic4.logistics.registry.builders.ClientBlockBuilder;
import com.restonic4.logistics.registry.entries.BlockEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class ClientBlockRegistry {
    private static final Map<BlockEntry<?, ?>, ClientBlockBuilder.RenderData> PENDING = new LinkedHashMap<>();
    public static final Map<Block, RenderType> BLOCKS_RENDER_TYPES = new HashMap<>();
    private static boolean renderTypeInjected = false;

    public static <BE extends BlockEntity> void register(BlockEntry<?, ?> entry, ClientBlockBuilder.RenderData data) {
        PENDING.put(entry, data);
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> void registerRenderer(
            BlockEntityType<T> type,
            Function<BlockEntityRendererProvider.Context, BlockEntityRenderer<?>> factory
    ) {
        BlockEntityRenderers.register(type, ctx -> (BlockEntityRenderer<T>) factory.apply(ctx));
    }

    @SuppressWarnings("unchecked")
    public static void apply() {
        for (Map.Entry<BlockEntry<?, ?>, ClientBlockBuilder.RenderData> e : PENDING.entrySet()) {
            BlockEntry<?, ?> entry = e.getKey();
            ClientBlockBuilder.RenderData data =  e.getValue();

            Block block = entry.getBlock();

            if (data.renderType() != null) {
                BLOCKS_RENDER_TYPES.put(block, data.renderType());
            }

            if (data.rendererFactory() != null && entry.hasBlockEntity()) {
                BlockEntityType<?> beType = entry.getBlockEntityTypeOrNull();
                if (beType != null) {
                    registerRenderer((BlockEntityType<BlockEntity>) beType, data.rendererFactory());
                }
            }
        }

        PENDING.clear();
    }

    public static boolean areRenderTypesInjected() {
        return renderTypeInjected;
    }

    public static void markRenderTypesAsInjected(Map<Block, RenderType> registry) {
        if (BLOCKS_RENDER_TYPES.isEmpty()) return;

        registry.putAll(BLOCKS_RENDER_TYPES);
        renderTypeInjected = true;
    }
}

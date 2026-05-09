package com.restonic4.logistics.registry.builders;

import com.restonic4.logistics.registry.ClientBlockRegistry;
import com.restonic4.logistics.registry.entries.BlockEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class ClientBlockBuilder {
    private final BlockEntry<?, ?> entry;

    private RenderType renderType = null;
    private Function<BlockEntityRendererProvider.Context, BlockEntityRenderer<?>> rendererFactory = null;

    private ClientBlockBuilder(BlockEntry<?, ?> entry) {
        this.entry = entry;
    }

    public static ClientBlockBuilder of(BlockEntry<?, ?> entry) {
        return new ClientBlockBuilder(entry);
    }

    public static ClientBlockBuilder of(BlockEntry<?, ?> entry, RenderType renderType) {
        return ClientBlockBuilder.of(entry).renderType(renderType);
    }

    public ClientBlockBuilder renderType(RenderType renderType) {
        this.renderType = renderType;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <BE extends BlockEntity> ClientBlockBuilder renderer(
            Class<BE> beClass,
            Function<BlockEntityRendererProvider.Context, BlockEntityRenderer<BE>> factory
    ) {
        this.rendererFactory = (Function<BlockEntityRendererProvider.Context, BlockEntityRenderer<?>>) (Function<?, ?>) factory;
        return this;
    }

    @SuppressWarnings("unchecked")
    public void register() {
        ClientBlockRegistry.register(entry, new RenderData(
                renderType,
                (Function<BlockEntityRendererProvider.Context, BlockEntityRenderer<?>>) (Function<?, ?>) rendererFactory
        ));
    }

    public record RenderData(
            @Nullable RenderType renderType,
            @Nullable Function<BlockEntityRendererProvider.Context, BlockEntityRenderer<?>> rendererFactory
    ) {}
}

package com.restonic4.logistics.registry;

import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public final class Registrate {
    private Registrate() {}

    public static <B extends Block, N extends NetworkNode> BlockBuilder<B, N> block(
            ResourceLocation id,
            Supplier<B> blockFactory
    ) {
        return new BlockBuilder<>(id, blockFactory);
    }
}

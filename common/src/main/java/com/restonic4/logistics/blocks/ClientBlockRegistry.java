package com.restonic4.logistics.blocks;

import com.restonic4.logistics.registry.builders.ClientBlockBuilder;
import net.minecraft.client.renderer.RenderType;

public class ClientBlockRegistry {
    public static void register() {
        ClientBlockBuilder.of(BlockRegistry.PIPE_BLOCK, RenderType.cutout()).register();
    }
}

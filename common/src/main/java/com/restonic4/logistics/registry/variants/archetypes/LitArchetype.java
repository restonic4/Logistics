package com.restonic4.logistics.registry.variants.archetypes;

import com.restonic4.logistics.registry.variants.GenerationContext;
import com.restonic4.logistics.registry.variants.VariantArchetype;

/**
 * A full cube driven by the vanilla {@code lit} boolean property — lamps and similar on/off
 * blocks. Generates two {@code cube_all} models ({@code <id>/off}, {@code <id>/on}), a blockstate
 * mapping {@code lit=false/true} to them, and an item model that inherits the "off" model.
 *
 * <p>Pairs with a block whose state definition adds {@link net.minecraft.world.level.block.state.properties.BlockStateProperties#LIT}.
 */
public final class LitArchetype implements VariantArchetype {
    private final String offTexture;
    private final String onTexture;

    public LitArchetype(String offTexture, String onTexture) {
        this.offTexture = offTexture;
        this.onTexture = onTexture;
    }

    @Override
    public void emit(GenerationContext ctx) {
        String offModel = ctx.blockModelRef("off");
        String onModel = ctx.blockModelRef("on");

        ctx.blockModel("off", "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\""
                + ctx.resolveTexture(offTexture) + "\"}}");
        ctx.blockModel("on", "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\""
                + ctx.resolveTexture(onTexture) + "\"}}");

        ctx.blockstate("{\"variants\":{"
                + "\"lit=false\":{\"model\":\"" + offModel + "\"},"
                + "\"lit=true\":{\"model\":\"" + onModel + "\"}"
                + "}}");

        ctx.itemModel("{\"parent\":\"" + offModel + "\"}");
    }
}

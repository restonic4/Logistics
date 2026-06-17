package com.restonic4.logistics.registry.variants.archetypes;

import com.restonic4.logistics.registry.variants.GenerationContext;
import com.restonic4.logistics.registry.variants.VariantArchetype;

/**
 * A non-directional full cube textured the same on every face — wallpapers, tiles, rugs, etc.
 * Generates a single-variant blockstate, a {@code cube_all} block model, and an item model that
 * inherits the block model.
 *
 * <p>The texture defaults to {@code <namespace>:block/<id>}; pass an explicit reference when the
 * texture lives elsewhere (e.g. {@code "logistics:block/wallpaper/red"}).
 */
public final class CubeAllArchetype implements VariantArchetype {
    private final String texture;

    public CubeAllArchetype(String texture) {
        this.texture = texture;
    }

    @Override
    public void emit(GenerationContext ctx) {
        String model = ctx.blockModelRef("");
        String tex = ctx.resolveTexture(texture);

        ctx.blockstate("{\"variants\":{\"\":{\"model\":\"" + model + "\"}}}");
        ctx.blockModel("{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"" + tex + "\"}}");
        ctx.itemModel("{\"parent\":\"" + model + "\"}");
    }
}

package com.restonic4.logistics.registry.variants.archetypes;

import com.restonic4.logistics.registry.variants.GenerationContext;
import com.restonic4.logistics.registry.variants.VariantArchetype;

/**
 * A non-directional full cube with one texture on the four sides and another on the top and
 * bottom — the {@code cube_bottom_top} parent. Used by the wood-trim wallpapers, and a natural
 * fit for future floors/roofs that want a distinct edge texture.
 */
public final class CubeBottomTopArchetype implements VariantArchetype {
    private final String sideTexture;
    private final String endTexture;

    public CubeBottomTopArchetype(String sideTexture, String endTexture) {
        this.sideTexture = sideTexture;
        this.endTexture = endTexture;
    }

    @Override
    public void emit(GenerationContext ctx) {
        String model = ctx.blockModelRef("");
        String side = ctx.resolveTexture(sideTexture);
        String end = ctx.resolveTexture(endTexture);

        ctx.blockstate("{\"variants\":{\"\":{\"model\":\"" + model + "\"}}}");
        ctx.blockModel("{\"parent\":\"minecraft:block/cube_bottom_top\",\"textures\":{"
                + "\"side\":\"" + side + "\","
                + "\"top\":\"" + end + "\","
                + "\"bottom\":\"" + end + "\"}}");
        ctx.itemModel("{\"parent\":\"" + model + "\"}");
    }
}

package com.restonic4.logistics.registry.variants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

/**
 * Handed to a {@link VariantArchetype} when a block is registered. Exposes the block id and a set
 * of typed helpers for scheduling generated client assets, so archetypes never touch
 * {@link VariantResources} or path conventions directly.
 */
public final class GenerationContext {
    private final ResourceLocation blockId;

    public GenerationContext(ResourceLocation blockId) {
        this.blockId = blockId;
    }

    public ResourceLocation id() {
        return blockId;
    }

    /** Writes {@code blockstates/<id>.json}. */
    public void blockstate(String json) {
        VariantResources.put(PackType.CLIENT_RESOURCES, VariantAssets.blockstatePath(blockId), json);
    }

    /** Writes the block's primary model {@code models/block/<id>.json}. */
    public void blockModel(String json) {
        blockModel("", json);
    }

    /** Writes a named block model {@code models/block/<id>/<suffix>.json} (suffix "" = primary). */
    public void blockModel(String suffix, String json) {
        VariantResources.put(PackType.CLIENT_RESOURCES, VariantAssets.blockModelPath(blockId, suffix), json);
    }

    /** Writes {@code models/item/<id>.json}. */
    public void itemModel(String json) {
        VariantResources.put(PackType.CLIENT_RESOURCES, VariantAssets.itemModelPath(blockId), json);
    }

    public String blockModelRef(String suffix) {
        return VariantAssets.blockModelRef(blockId, suffix);
    }

    public String resolveTexture(String texture) {
        return VariantAssets.resolveTexture(blockId, texture);
    }
}

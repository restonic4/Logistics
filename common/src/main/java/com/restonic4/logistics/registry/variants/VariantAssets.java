package com.restonic4.logistics.registry.variants;

import net.minecraft.resources.ResourceLocation;

/**
 * Path conventions shared by the variant system: maps a block id to the in-pack file location of
 * each generated resource, and builds the model references used inside generated JSON.
 */
public final class VariantAssets {
    private VariantAssets() {}

    public static ResourceLocation blockstatePath(ResourceLocation blockId) {
        return new ResourceLocation(blockId.getNamespace(), "blockstates/" + blockId.getPath() + ".json");
    }

    public static ResourceLocation blockModelPath(ResourceLocation blockId, String suffix) {
        String path = suffix.isEmpty() ? blockId.getPath() : blockId.getPath() + "/" + suffix;
        return new ResourceLocation(blockId.getNamespace(), "models/block/" + path + ".json");
    }

    public static ResourceLocation itemModelPath(ResourceLocation blockId) {
        return new ResourceLocation(blockId.getNamespace(), "models/item/" + blockId.getPath() + ".json");
    }

    public static ResourceLocation recipePath(ResourceLocation blockId, String suffix) {
        return new ResourceLocation(blockId.getNamespace(), "recipes/" + blockId.getPath() + suffix + ".json");
    }

    /** Reference to a generated block model, e.g. {@code logistics:block/red_wallpaper}. */
    public static String blockModelRef(ResourceLocation blockId, String suffix) {
        String path = suffix.isEmpty() ? blockId.getPath() : blockId.getPath() + "/" + suffix;
        return blockId.getNamespace() + ":block/" + path;
    }

    /** Resolves a texture reference, defaulting an unqualified path to the block's namespace. */
    public static String resolveTexture(ResourceLocation blockId, String texture) {
        if (texture == null) {
            return blockId.getNamespace() + ":block/" + blockId.getPath();
        }
        return texture.contains(":") ? texture : blockId.getNamespace() + ":" + texture;
    }
}

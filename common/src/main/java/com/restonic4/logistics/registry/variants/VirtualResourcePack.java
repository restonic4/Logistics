package com.restonic4.logistics.registry.variants;

import com.restonic4.logistics.Constants;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * A synthetic, in-memory {@link PackResources} that serves the resources scheduled in
 * {@link VariantResources}. It backs a single always-on pack pinned to the bottom of the pack
 * stack (see {@link VariantPackSource}), so any real hand-authored JSON in the mod's resources
 * transparently overrides what we generate here.
 *
 * <p>One instance serves both {@link PackType#CLIENT_RESOURCES} (blockstates/models) and
 * {@link PackType#SERVER_DATA} (recipes); the {@code PackType} argument on each method selects
 * the right bucket, so the same pack works in both the client and server pack repositories.
 */
public final class VirtualResourcePack implements PackResources {
    public static final String PACK_ID = Constants.MOD_ID + ":generated";

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        // pack.mcmeta is provided via getMetadataSection instead of a root file.
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        VariantResources.freeze();
        byte[] bytes = VariantResources.view(type).get(location);
        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void listResources(PackType type, String namespace, String pathPrefix, ResourceOutput output) {
        VariantResources.freeze();
        VariantResources.view(type).forEach((location, bytes) -> {
            if (location.getNamespace().equals(namespace) && location.getPath().startsWith(pathPrefix)) {
                output.accept(location, () -> new ByteArrayInputStream(bytes));
            }
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        VariantResources.freeze();
        Set<String> namespaces = new HashSet<>();
        for (ResourceLocation location : VariantResources.view(type).keySet()) {
            namespaces.add(location.getNamespace());
        }
        return namespaces;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        if (serializer == PackMetadataSection.TYPE) {
            // RESOURCE_PACK_FORMAT == DATA_PACK_FORMAT on 1.20.1, so one value is valid for both.
            return (T) new PackMetadataSection(
                    Component.literal("Logistics generated variant resources"),
                    SharedConstants.RESOURCE_PACK_FORMAT
            );
        }
        return null;
    }

    @Override
    public String packId() {
        return PACK_ID;
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public void close() {}
}

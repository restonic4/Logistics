package com.restonic4.logistics.registry.variants;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.flag.FeatureFlagSet;

import java.util.function.Consumer;

/**
 * Supplies the single generated pack to a {@link net.minecraft.server.packs.repository.PackRepository}.
 * Injected into every repository (client and server) by {@code PackRepositoryMixin}.
 *
 * <p>The pack is {@code required} (always selected, cannot be disabled) and pinned to
 * {@link Pack.Position#BOTTOM}, giving it the lowest priority in the stack — so real files from
 * the mod's own resources, resource packs, or datapacks always win over generated ones.
 */
public final class VariantPackSource implements RepositorySource {
    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        Pack pack = Pack.create(
                VirtualResourcePack.PACK_ID,
                Component.literal("Logistics Generated"),
                true, // required -> always selected
                id -> new VirtualResourcePack(),
                new Pack.Info(
                        Component.literal("Logistics generated variant resources"),
                        SharedConstants.RESOURCE_PACK_FORMAT,
                        FeatureFlagSet.of()
                ),
                // Only used to compute pack-format compatibility; format 15 is valid for both
                // CLIENT_RESOURCES and SERVER_DATA on 1.20.1, so this one Pack serves both repos.
                PackType.CLIENT_RESOURCES,
                Pack.Position.BOTTOM,
                false, // not fixed-position: users may reorder packs above it freely
                PackSource.BUILT_IN
        );
        consumer.accept(pack);
    }
}

package com.restonic4.logistics.registry.variants;

import com.restonic4.logistics.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory buffer of runtime-generated resources (blockstates, models, recipes...). It is
 * populated at block-registration time and served to the game by {@link VirtualResourcePack}.
 *
 * <p>This mirrors the schedule/freeze contract already used by {@code PlatformRegistry} for the
 * loot-table and block-tag injections: everything is scheduled up-front (before the first
 * resource reload), then frozen the first time the pack is read.
 *
 * <p>Keys are the in-pack file locations, e.g. {@code logistics:blockstates/red_wallpaper.json}
 * or {@code logistics:recipes/red_wallpaper_from_blue_wallpaper_stonecutting.json}.
 */
public final class VariantResources {
    private VariantResources() {}

    private static final Map<PackType, Map<ResourceLocation, byte[]>> RESOURCES = new EnumMap<>(PackType.class);
    private static boolean frozen = false;

    static {
        for (PackType type : PackType.values()) {
            RESOURCES.put(type, new LinkedHashMap<>());
        }
    }

    /** Schedules a generated JSON resource. Must be called before the first resource reload. */
    public static void put(PackType type, ResourceLocation path, String json) {
        if (frozen) {
            throw new IllegalStateException("VariantResources is frozen; cannot schedule " + path
                    + ". All variant resources must be registered during block registration.");
        }
        byte[] previous = RESOURCES.get(type).put(path, json.getBytes(StandardCharsets.UTF_8));
        if (previous != null) {
            Constants.LOG.warn("Generated resource overwritten: {} ({})", path, type);
        }
    }

    /** Live view of the scheduled resources for a pack type. Used by {@link VirtualResourcePack}. */
    public static Map<ResourceLocation, byte[]> view(PackType type) {
        return RESOURCES.get(type);
    }

    /** Freezes scheduling. Called lazily the first time the virtual pack is read. */
    public static void freeze() {
        if (!frozen) {
            frozen = true;
            Constants.LOG.info("Froze variant resources: {} client asset(s), {} server data file(s).",
                    RESOURCES.get(PackType.CLIENT_RESOURCES).size(),
                    RESOURCES.get(PackType.SERVER_DATA).size());
        }
    }

    public static boolean isFrozen() {
        return frozen;
    }
}

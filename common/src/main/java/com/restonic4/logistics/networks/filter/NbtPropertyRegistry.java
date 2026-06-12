package com.restonic4.logistics.networks.filter;

import com.restonic4.logistics.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static registry of every available {@link NbtProperty}. {@link NbtRule}s reference
 * properties by ID, so anything written to disk or network must be registered here
 * before it can be evaluated.
 */
public final class NbtPropertyRegistry {
    private static final Map<ResourceLocation, NbtProperty> REGISTRY = new LinkedHashMap<>();

    // Built-in properties
    public static final NbtProperty ENERGY = register(
            new EnergyNbtProperty(new ResourceLocation(Constants.MOD_ID, "energy")));
    public static final NbtProperty DURABILITY = register(
            new DurabilityNbtProperty(new ResourceLocation(Constants.MOD_ID, "durability")));

    private NbtPropertyRegistry() {}

    /**
     * Registers a property. Intended to be called once per property during static initialization.
     *
     * @throws IllegalStateException if the ID is already taken
     */
    public static <P extends NbtProperty> P register(P property) {
        if (REGISTRY.putIfAbsent(property.getId(), property) != null) {
            throw new IllegalStateException("Duplicate NBT property registration: " + property.getId());
        }
        return property;
    }

    /** Looks up a property by its ID, or {@code null} if unknown. */
    public static NbtProperty get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    /** All registered properties, in registration order (e.g. for UI listings). */
    public static Collection<NbtProperty> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /** The registered properties that are meaningful for the given stack. */
    public static List<NbtProperty> propertiesFor(ItemStack stack) {
        List<NbtProperty> result = new ArrayList<>();
        for (NbtProperty property : REGISTRY.values()) {
            if (property.appliesTo(stack)) result.add(property);
        }
        return result;
    }
}

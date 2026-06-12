package com.restonic4.logistics.networks.filter;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A registered, named numeric value that can be read out of an {@link ItemStack}'s NBT
 * (stored energy, durability, ...). Properties are what give {@link NbtRule}s semantic
 * comparisons ("energy >= 50%") over otherwise opaque tags: arbitrary NBT that has no
 * registered property can still be matched via {@link ItemFilter.NbtMode#ANY} or
 * {@link ItemFilter.NbtMode#EXACT}, but only registered properties support range rules.
 * <p>
 * Register implementations in {@link NbtPropertyRegistry}.
 */
public abstract class NbtProperty {
    private final ResourceLocation id;

    protected NbtProperty(ResourceLocation id) {
        this.id = id;
    }

    /** The unique identifier of this property, written to disk and network by {@link NbtRule}. */
    public final ResourceLocation getId() { return id; }

    /** Human-readable name (lang key {@code nbt_property.<namespace>.<path>}), used by UIs. */
    public Component getDisplayName() {
        return Component.translatable("nbt_property." + id.getNamespace() + "." + id.getPath());
    }

    /** Whether this property is meaningful for the given stack (e.g. "is an energy item"). */
    public abstract boolean appliesTo(ItemStack stack);

    /** The current numeric value of this property on the stack. */
    public abstract double getValue(ItemStack stack);

    /**
     * The maximum the value can reach on this stack, used to normalize percent-based
     * rules. Return {@code <= 0} if there is no meaningful maximum; percent comparisons
     * then never match for this property.
     */
    public abstract double getMaxValue(ItemStack stack);

    /** Short value text for UI labels, e.g. {@code "750/1000 (75%)"}. */
    public String describeValue(ItemStack stack) {
        long value = (long) getValue(stack);
        double max = getMaxValue(stack);
        if (max > 0) {
            return value + "/" + (long) max + " (" + Math.round(value * 100.0 / max) + "%)";
        }
        return String.valueOf(value);
    }
}

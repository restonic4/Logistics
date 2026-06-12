package com.restonic4.logistics.networks.filter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * One numeric condition over a registered {@link NbtProperty}, e.g. "energy &gt;= 50%".
 * The threshold is compared either against the property's percent fill (value / max * 100)
 * or its raw value. A rule never matches stacks its property does not apply to.
 */
public final class NbtRule {
    private static final String TAG_PROPERTY = "property";
    private static final String TAG_COMPARISON = "comparison";
    private static final String TAG_THRESHOLD = "threshold";
    private static final String TAG_PERCENT = "percent";

    private static final double EQUAL_EPSILON = 1.0E-6;

    public enum Comparison {
        LESS("<"),
        LESS_EQUAL("≤"),
        EQUAL("="),
        GREATER_EQUAL("≥"),
        GREATER(">");

        private final String symbol;

        Comparison(String symbol) {
            this.symbol = symbol;
        }

        public String symbol() { return symbol; }
    }

    private ResourceLocation propertyId = NbtPropertyRegistry.ENERGY.getId();
    private Comparison comparison = Comparison.GREATER_EQUAL;
    private double threshold = 50.0D;
    private boolean percent = true;

    public ResourceLocation getPropertyId() { return propertyId; }
    public void setPropertyId(ResourceLocation propertyId) { this.propertyId = propertyId; }

    public Comparison getComparison() { return comparison; }
    public void setComparison(Comparison comparison) { this.comparison = comparison; }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }

    /** Whether the threshold is a percent of the property's max instead of a raw value. */
    public boolean isPercent() { return percent; }
    public void setPercent(boolean percent) { this.percent = percent; }

    public boolean matches(ItemStack stack) {
        NbtProperty property = NbtPropertyRegistry.get(propertyId);
        if (property == null || !property.appliesTo(stack)) return false;

        double value = property.getValue(stack);
        if (percent) {
            double max = property.getMaxValue(stack);
            if (max <= 0) return false;
            value = value / max * 100.0D;
        }

        return switch (comparison) {
            case LESS -> value < threshold;
            case LESS_EQUAL -> value <= threshold;
            case EQUAL -> Math.abs(value - threshold) < EQUAL_EPSILON;
            case GREATER_EQUAL -> value >= threshold;
            case GREATER -> value > threshold;
        };
    }

    /** Short text for summaries/logs, e.g. {@code "energy >= 50%"}. */
    public String describe() {
        String name = propertyId.getPath();
        String value = threshold == Math.floor(threshold) ? String.valueOf((long) threshold) : String.valueOf(threshold);
        return name + " " + comparison.symbol() + " " + value + (percent ? "%" : "");
    }

    public void save(CompoundTag tag) {
        tag.putString(TAG_PROPERTY, propertyId.toString());
        tag.putString(TAG_COMPARISON, comparison.name());
        tag.putDouble(TAG_THRESHOLD, threshold);
        tag.putBoolean(TAG_PERCENT, percent);
    }

    public void load(CompoundTag tag) {
        this.propertyId = new ResourceLocation(tag.getString(TAG_PROPERTY));
        this.comparison = Comparison.valueOf(tag.getString(TAG_COMPARISON));
        this.threshold = tag.getDouble(TAG_THRESHOLD);
        this.percent = tag.getBoolean(TAG_PERCENT);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(propertyId);
        buf.writeEnum(comparison);
        buf.writeDouble(threshold);
        buf.writeBoolean(percent);
    }

    public void read(FriendlyByteBuf buf) {
        this.propertyId = buf.readResourceLocation();
        this.comparison = buf.readEnum(Comparison.class);
        this.threshold = buf.readDouble();
        this.percent = buf.readBoolean();
    }

    public static NbtRule fromTag(CompoundTag tag) {
        NbtRule rule = new NbtRule();
        rule.load(tag);
        return rule;
    }

    public static NbtRule fromBuf(FriendlyByteBuf buf) {
        NbtRule rule = new NbtRule();
        rule.read(buf);
        return rule;
    }
}

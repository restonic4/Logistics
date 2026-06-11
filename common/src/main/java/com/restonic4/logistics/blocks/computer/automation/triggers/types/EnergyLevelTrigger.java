package com.restonic4.logistics.blocks.computer.automation.triggers.types;

import com.restonic4.logistics.blocks.computer.automation.triggers.core.Trigger;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.TriggerRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

/**
 * Fires based on the fill level of the computer's energy network.
 * <p>
 * The condition holds when the network's stored energy percentage is above or below
 * (per {@link Comparison}) the configured threshold. Combined with
 * {@link ExecutionMode#ONCE_UNTIL_FALSE} this gives classic "low battery alarm"
 * behavior: fire once when energy drops under the threshold, re-arm once it recovers.
 */
public class EnergyLevelTrigger extends Trigger {
    private static final String TAG_THRESHOLD = "thresholdPercent";
    private static final String TAG_COMPARISON = "comparison";

    /** How the measured energy percentage is compared against the threshold. */
    public enum Comparison {
        /** Condition holds while energy percent is strictly above the threshold. */
        ABOVE,
        /** Condition holds while energy percent is strictly below the threshold. */
        BELOW
    }

    private double thresholdPercent = 50.0D;
    private Comparison comparison = Comparison.BELOW;

    public EnergyLevelTrigger() {
        super(TriggerRegistry.ENERGY_LEVEL);
    }

    /** The threshold in percent (0-100) the network fill level is compared against. */
    public double getThresholdPercent() { return thresholdPercent; }

    public void setThresholdPercent(double thresholdPercent) {
        this.thresholdPercent = Mth.clamp(thresholdPercent, 0.0D, 100.0D);
    }

    public Comparison getComparison() { return comparison; }
    public void setComparison(Comparison comparison) { this.comparison = comparison; }

    @Override
    public boolean evaluate(TriggerContext ctx) {
        double energyPercent = ctx.getEnergyPercent();
        return switch (comparison) {
            case ABOVE -> energyPercent > thresholdPercent;
            case BELOW -> energyPercent < thresholdPercent;
        };
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putDouble(TAG_THRESHOLD, thresholdPercent);
        tag.putString(TAG_COMPARISON, comparison.name());
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        this.thresholdPercent = tag.getDouble(TAG_THRESHOLD);
        this.comparison = Comparison.valueOf(tag.getString(TAG_COMPARISON));
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        buf.writeDouble(thresholdPercent);
        buf.writeEnum(comparison);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        this.thresholdPercent = buf.readDouble();
        this.comparison = buf.readEnum(Comparison.class);
    }
}

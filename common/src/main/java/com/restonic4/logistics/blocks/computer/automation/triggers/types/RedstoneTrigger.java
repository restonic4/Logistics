package com.restonic4.logistics.blocks.computer.automation.triggers.types;

import com.restonic4.logistics.blocks.computer.automation.triggers.core.Trigger;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.TriggerRegistry;
import com.restonic4.logistics.blocks.computer.automation.triggers.util.NetworkNodeTarget;
import com.restonic4.logistics.blocks.redstone_reader.RedstoneReaderNode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Fires based on the redstone signal seen by targeted redstone readers.
 * <p>
 * The condition holds when any targeted reader matches: it is powered, unpowered, or its strength
 * compares (per {@link Comparison}) against a 0-15 level. Pairing this with
 * {@link ExecutionMode#ONCE_UNTIL_FALSE} vs {@link ExecutionMode#CONTINUOUS} gives the whole family
 * of behaviors: a single pulse on the rising edge ({@code POWERED} + once), "while powered" every
 * tick ({@code POWERED} + continuous), a one-shot when the signal drops ({@code UNPOWERED} + once),
 * or analog thresholds ({@code STRENGTH}).
 */
public class RedstoneTrigger extends Trigger {
    private static final String TAG_CONDITION = "condition";
    private static final String TAG_COMPARISON = "comparison";
    private static final String TAG_LEVEL = "level";

    /** What aspect of the reader's signal the condition checks. */
    public enum Condition {
        /** Holds while any targeted reader sees power (strength &gt; 0). */
        POWERED,
        /** Holds while no targeted reader sees power. */
        UNPOWERED,
        /** Holds while any targeted reader's strength compares against {@link #getLevel()}. */
        STRENGTH
    }

    /** How a reader's strength is compared against the configured level (for {@link Condition#STRENGTH}). */
    public enum Comparison {
        /** Strength &gt;= level. */
        AT_LEAST,
        /** Strength &lt;= level. */
        AT_MOST,
        /** Strength == level. */
        EQUAL
    }

    private final NetworkNodeTarget<RedstoneReaderNode> target = new NetworkNodeTarget<>();
    private Condition condition = Condition.POWERED;
    private Comparison comparison = Comparison.AT_LEAST;
    private int level = 15;

    public RedstoneTrigger() {
        super(TriggerRegistry.REDSTONE);
    }

    public NetworkNodeTarget<RedstoneReaderNode> getTarget() { return target; }

    public Condition getCondition() { return condition; }
    public void setCondition(Condition condition) { this.condition = condition; }

    public Comparison getComparison() { return comparison; }
    public void setComparison(Comparison comparison) { this.comparison = comparison; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Mth.clamp(level, 0, 15); }

    @Override
    public boolean evaluate(TriggerContext ctx) {
        List<RedstoneReaderNode> readers = target.resolve(ctx.getRedstoneReaders());
        if (readers.isEmpty()) return false;

        if (condition == Condition.UNPOWERED) {
            for (RedstoneReaderNode reader : readers) {
                if (reader.isPowered()) return false;
            }
            return true;
        }

        for (RedstoneReaderNode reader : readers) {
            if (matches(reader)) return true;
        }
        return false;
    }

    private boolean matches(RedstoneReaderNode reader) {
        return switch (condition) {
            case POWERED -> reader.isPowered();
            case UNPOWERED -> !reader.isPowered(); // handled above, kept for completeness
            case STRENGTH -> switch (comparison) {
                case AT_LEAST -> reader.getSignalStrength() >= level;
                case AT_MOST -> reader.getSignalStrength() <= level;
                case EQUAL -> reader.getSignalStrength() == level;
            };
        };
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        target.save(tag);
        tag.putString(TAG_CONDITION, condition.name());
        tag.putString(TAG_COMPARISON, comparison.name());
        tag.putInt(TAG_LEVEL, level);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        target.load(tag);
        this.condition = Condition.valueOf(tag.getString(TAG_CONDITION));
        this.comparison = Comparison.valueOf(tag.getString(TAG_COMPARISON));
        this.level = tag.getInt(TAG_LEVEL);
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        target.write(buf);
        buf.writeEnum(condition);
        buf.writeEnum(comparison);
        buf.writeVarInt(level);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        target.read(buf);
        this.condition = buf.readEnum(Condition.class);
        this.comparison = buf.readEnum(Comparison.class);
        this.level = buf.readVarInt();
    }
}

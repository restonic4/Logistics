package com.restonic4.logistics.blocks.computer.automation.triggers.types;

import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.Trigger;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.TriggerRegistry;
import com.restonic4.logistics.blocks.computer.automation.triggers.util.AccessorTarget;
import com.restonic4.logistics.networks.filter.ItemFilter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Fires based on how many items matching an {@link ItemFilter} sit in the scoped accessor(s):
 * one specific accessor, or every accessor bridged to the computer's network combined.
 * <p>
 * The classic auto-restock setup pairs this with {@code SendItemsAction}: "when the supply
 * chest holds below 200 cobblestone, top it back up". Counting goes through the accessors'
 * virtual inventories, so it works even while the target chunks are unloaded.
 */
public class ItemCountTrigger extends Trigger {
    private static final String TAG_FILTER = "filter";
    private static final String TAG_COMPARISON = "comparison";
    private static final String TAG_THRESHOLD = "threshold";

    /** How the measured item count is compared against the threshold. */
    public enum Comparison {
        /** Condition holds while the count is strictly below the threshold. */
        BELOW,
        /** Condition holds while the count is strictly above the threshold. */
        ABOVE
    }

    private final AccessorTarget scope = new AccessorTarget();
    private ItemFilter filter = new ItemFilter();
    private Comparison comparison = Comparison.BELOW;
    private int threshold = 200;

    public ItemCountTrigger() {
        super(TriggerRegistry.ITEM_COUNT);
    }

    /** The accessor(s) whose contents are counted; "any" means all of them combined. */
    public AccessorTarget getScope() { return scope; }

    public ItemFilter getFilter() { return filter; }
    public void setFilter(ItemFilter filter) { this.filter = filter; }

    public Comparison getComparison() { return comparison; }
    public void setComparison(Comparison comparison) { this.comparison = comparison; }

    public int getThreshold() { return threshold; }
    public void setThreshold(int threshold) { this.threshold = Math.max(0, threshold); }

    @Override
    public boolean evaluate(TriggerContext ctx) {
        if (!filter.isValid()) return false;

        int count = 0;
        for (AccessorNode accessor : scope.resolve(ctx)) {
            count += accessor.countMatching(filter, ctx.getLevel());
        }

        return switch (comparison) {
            case BELOW -> count < threshold;
            case ABOVE -> count > threshold;
        };
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        scope.save(tag);
        CompoundTag filterTag = new CompoundTag();
        filter.save(filterTag);
        tag.put(TAG_FILTER, filterTag);
        tag.putString(TAG_COMPARISON, comparison.name());
        tag.putInt(TAG_THRESHOLD, threshold);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        scope.load(tag);
        this.filter = ItemFilter.fromTag(tag.getCompound(TAG_FILTER));
        this.comparison = Comparison.valueOf(tag.getString(TAG_COMPARISON));
        this.threshold = tag.getInt(TAG_THRESHOLD);
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        scope.write(buf);
        filter.write(buf);
        buf.writeEnum(comparison);
        buf.writeVarInt(threshold);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        scope.read(buf);
        this.filter = ItemFilter.fromBuf(buf);
        this.comparison = buf.readEnum(Comparison.class);
        this.threshold = buf.readVarInt();
    }
}

package com.restonic4.logistics.blocks.computer.automation.triggers.actions;

import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.blocks.computer.ComputerLogEntry;
import com.restonic4.logistics.blocks.computer.ComputerLogger;
import com.restonic4.logistics.blocks.computer.ItemTransferService;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.ActionExecutionContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.ExecuteResult;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerAction;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.ActionRegistry;
import com.restonic4.logistics.blocks.computer.automation.triggers.util.AccessorTarget;
import com.restonic4.logistics.networks.filter.ItemFilter;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * Ships items matching an {@link ItemFilter} to a target accessor through
 * {@link ItemTransferService} — the same path the Transfer tab uses, so energy billing,
 * NBT handling and parcel trails behave identically.
 * <p>
 * Two send modes:
 * <ul>
 *   <li>{@link Mode#FIXED} — send the configured amount.</li>
 *   <li>{@link Mode#TOP_UP} — restock: send only the difference between the configured
 *       amount and what the target already holds. Paired with {@code ItemCountTrigger}
 *       this is the "keep this chest at N items" recipe.</li>
 * </ul>
 * Both modes send partially when the source holds less than the requested amount —
 * automation runs unattended, so a shortfall must not strand the remainder (a recurring
 * "send 64" against a chest holding 30 still moves the 30). Items already in flight toward
 * the target count as delivered for the top-up deficit, so a fast re-firing trigger doesn't
 * over-send while parcels are airborne. A top-up that finds the target already covered is
 * a successful no-op.
 * <p>
 * By default a transfer that moves nothing aborts the running sequence
 * ({@link ExecuteResult#FAIL}); the stop-on-failure toggle lets the sequence carry on past
 * a failed send instead. Logging to the computer console can also be disabled per action.
 */
public class SendItemsAction extends TriggerAction {
    private static final String TAG_FILTER = "filter";
    private static final String TAG_TARGET = "targetAccessorId";
    private static final String TAG_MODE = "sendMode";
    private static final String TAG_AMOUNT = "amount";
    private static final String TAG_LOG = "logTransfers";
    private static final String TAG_STOP_ON_FAILURE = "stopOnFailure";

    public static final int MAX_AMOUNT = 1024;

    public enum Mode {
        /** Send exactly the configured amount. */
        FIXED,
        /** Send only what the target is missing to reach the configured amount. */
        TOP_UP
    }

    private final AccessorTarget source = new AccessorTarget();
    private UUID targetAccessorId = null;
    private ItemFilter filter = new ItemFilter();
    private Mode mode = Mode.FIXED;
    private int amount = 64;
    private boolean logTransfers = true;
    private boolean stopOnFailure = true;

    public SendItemsAction() {
        super(ActionRegistry.SEND_ITEMS);
    }

    /** Where the items come from; "any" lets the service auto-pick the best-stocked accessor. */
    public AccessorTarget getSource() { return source; }

    public UUID getTargetAccessorId() { return targetAccessorId; }
    public void setTargetAccessorId(UUID targetAccessorId) { this.targetAccessorId = targetAccessorId; }

    public ItemFilter getFilter() { return filter; }
    public void setFilter(ItemFilter filter) { this.filter = filter; }

    public Mode getSendMode() { return mode; }
    public void setSendMode(Mode mode) { this.mode = mode; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = Math.max(1, Math.min(amount, MAX_AMOUNT)); }

    /** Whether transfer outcomes are written to the computer's log. */
    public boolean isLogTransfers() { return logTransfers; }
    public void setLogTransfers(boolean logTransfers) { this.logTransfers = logTransfers; }

    /** Whether a failed send aborts the running sequence or lets the following actions run. */
    public boolean isStopOnFailure() { return stopOnFailure; }
    public void setStopOnFailure(boolean stopOnFailure) { this.stopOnFailure = stopOnFailure; }

    @Override
    public ExecuteResult execute(TriggerContext ctx, ActionExecutionContext runCtx) {
        EnergyNetwork network = ctx.getNode().getNetwork();
        if (network == null) return failResult();

        AccessorNode target = targetAccessorId != null ? ctx.findAccessor(targetAccessorId) : null;
        if (target == null) {
            ComputerLogger.log(ctx.getLevel(), ctx.getBlockPos(), ComputerLogEntry.Severity.WARN,
                    "Send items failed: target accessor no longer exists.");
            return failResult();
        }

        int quantity = amount;
        if (mode == Mode.TOP_UP) {
            // Items already flying toward the target count as delivered, so re-fires
            // while parcels are airborne don't stack up extra sends.
            int present = target.countMatching(filter, ctx.getLevel());
            int inFlight = target.getNetwork() != null
                    ? target.getNetwork().countInFlightTo(target.getBlockPos(), filter)
                    : 0;
            quantity = amount - present - inFlight;
            if (quantity <= 0) return ExecuteResult.SUCCESS;
        }

        BlockPos fromPos = null;
        if (!source.isAny()) {
            AccessorNode sourceAccessor = source.getAccessorId() != null ? ctx.findAccessor(source.getAccessorId()) : null;
            if (sourceAccessor == null) {
                ComputerLogger.log(ctx.getLevel(), ctx.getBlockPos(), ComputerLogEntry.Severity.WARN,
                        "Send items failed: source accessor no longer exists.");
                return failResult();
            }
            fromPos = sourceAccessor.getBlockPos();
        }

        ItemTransferService.Result result = ItemTransferService.transfer(
                ctx.getLevel(), network, ctx.getBlockPos(),
                filter, quantity, fromPos, target.getBlockPos(),
                true, logTransfers);

        return result.success() ? ExecuteResult.SUCCESS : failResult();
    }

    /** How a failed send affects the owning sequence, per the stop-on-failure toggle. */
    private ExecuteResult failResult() {
        return stopOnFailure ? ExecuteResult.FAIL : ExecuteResult.SUCCESS;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        source.save(tag);
        if (targetAccessorId != null) tag.putUUID(TAG_TARGET, targetAccessorId);
        CompoundTag filterTag = new CompoundTag();
        filter.save(filterTag);
        tag.put(TAG_FILTER, filterTag);
        tag.putString(TAG_MODE, mode.name());
        tag.putInt(TAG_AMOUNT, amount);
        tag.putBoolean(TAG_LOG, logTransfers);
        tag.putBoolean(TAG_STOP_ON_FAILURE, stopOnFailure);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        source.load(tag);
        this.targetAccessorId = tag.hasUUID(TAG_TARGET) ? tag.getUUID(TAG_TARGET) : null;
        this.filter = ItemFilter.fromTag(tag.getCompound(TAG_FILTER));
        this.mode = Mode.valueOf(tag.getString(TAG_MODE));
        this.amount = tag.getInt(TAG_AMOUNT);
        this.logTransfers = !tag.contains(TAG_LOG) || tag.getBoolean(TAG_LOG);
        this.stopOnFailure = !tag.contains(TAG_STOP_ON_FAILURE) || tag.getBoolean(TAG_STOP_ON_FAILURE);
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        source.write(buf);
        buf.writeBoolean(targetAccessorId != null);
        if (targetAccessorId != null) buf.writeUUID(targetAccessorId);
        filter.write(buf);
        buf.writeEnum(mode);
        buf.writeVarInt(amount);
        buf.writeBoolean(logTransfers);
        buf.writeBoolean(stopOnFailure);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        source.read(buf);
        this.targetAccessorId = buf.readBoolean() ? buf.readUUID() : null;
        this.filter = ItemFilter.fromBuf(buf);
        this.mode = buf.readEnum(Mode.class);
        this.amount = buf.readVarInt();
        this.logTransfers = buf.readBoolean();
        this.stopOnFailure = buf.readBoolean();
    }
}

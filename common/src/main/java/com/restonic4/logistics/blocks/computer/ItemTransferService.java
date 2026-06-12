package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.filter.ItemFilter;
import com.restonic4.logistics.networks.pathfinding.Parcel;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.networks.types.ItemNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * The one place a computer turns "send N items matching this filter to that accessor" into
 * source selection, energy billing, extraction, and parcel dispatch. Used by the Transfer
 * tab ({@link ComputerTransferPacket}) and by automation ({@code SendItemsAction}), so both
 * paths behave identically and log to the computer the same way.
 * <p>
 * Transfers never overflow the destination: the quantity is clamped to what the target
 * container can still hold, counting items already in flight toward it (parcels and trail
 * queues) as occupying that space. Items with distinct NBT extract into separate stacks;
 * transfers larger than one stack ship as a parcel trail (one parcel per 0.5s) via
 * {@link ItemNetwork#requestTransfer}.
 */
public final class ItemTransferService {

    public record Result(boolean success, int itemsSent, int parcelCount) {
        public static final Result FAILED = new Result(false, 0, 0);
    }

    private ItemTransferService() {}

    /**
     * Performs a transfer on behalf of the given computer. Must run on the server thread
     * (inside the network's {@code execute} or a node tick).
     *
     * @param fromPos      a specific source accessor, or {@code null} to auto-pick one
     * @param allowPartial when {@code true}, sends whatever the chosen source has (up to
     *                     {@code quantity}) instead of failing on a shortfall
     * @param log          whether to write outcomes to the computer's log (automation may
     *                     turn this off to avoid spamming the console every firing)
     */
    public static Result transfer(ServerLevel level, EnergyNetwork energyNetwork, BlockPos computerPos,
                                  ItemFilter filter, int quantity, @Nullable BlockPos fromPos, BlockPos targetPos,
                                  boolean allowPartial, boolean log) {
        if (quantity <= 0) return Result.FAILED;

        if (!filter.isValid() || filter.getItem() == Items.AIR) {
            log(log, level, computerPos, ComputerLogEntry.Severity.WARN,
                    "Transfer failed: no valid item selected.");
            return Result.FAILED;
        }

        // ── Resolve the destination. Parcels can only travel within one item network, so a
        //    transfer is impossible without knowing the target's network: every source must
        //    come from that same network or the parcel would have no route and drop. ──
        AccessorNode targetNode = null;
        ItemNetwork targetNetwork = null;
        Network resolvedTarget = NetworkManager.get(level).getNetworkByBlockPos(targetPos);
        if (resolvedTarget instanceof ItemNetwork itemNet
                && itemNet.getNodeIndex().findByBlockPos(targetPos) instanceof AccessorNode acc) {
            targetNode = acc;
            targetNetwork = itemNet;
        }
        if (targetNode == null) {
            log(log, level, computerPos, ComputerLogEntry.Severity.WARN,
                    String.format("Transfer failed: target %s is not an accessor on an item network.",
                            targetPos.toShortString()));
            return Result.FAILED;
        }

        // ── Capacity gate: never ship more than the target can hold right now.
        //    Items already flying toward it count as occupying that space. ──
        int inFlight = targetNetwork.countInFlightTo(targetPos, filter);
        int acceptable = targetNode.countAcceptable(filter, filter.createDisplayStack(), level) - inFlight;
        if (acceptable <= 0) {
            log(log, level, computerPos, ComputerLogEntry.Severity.WARN,
                    String.format("Transfer skipped: target %s cannot hold any more %s%s.",
                            targetPos.toShortString(), filter.describe(),
                            inFlight > 0 ? " (" + inFlight + " already in flight)" : ""));
            return Result.FAILED;
        }
        quantity = Math.min(quantity, acceptable);

        AccessorNode sourceNode = null;

        // ── MANUAL: user picked a specific input accessor ──
        if (fromPos != null) {
            // Resolving through the target's network enforces same-network routing: a source
            // on a different item network simply isn't found there.
            if (targetNetwork.getNodeIndex().findByBlockPos(fromPos) instanceof AccessorNode acc) {
                sourceNode = acc;
            } else {
                log(log, level, computerPos, ComputerLogEntry.Severity.WARN,
                        String.format("Transfer failed: source %s is not on the same item network as target %s.",
                                fromPos.toShortString(), targetPos.toShortString()));
                return Result.FAILED;
            }
        }
        // ── AUTO: pick the best-stocked accessor on the target's own network ──
        else {
            // The destination must never feed itself: exclude the target accessor AND any other
            // accessor reading the same container, or the transfer loops the chest's contents
            // back into it forever, draining energy until the network locks up.
            BlockPos targetContainer = targetNode.getContainerPos();

            int bestAvailable = 0;
            for (var netNode : targetNetwork.getNodeIndex().getAllNodes()) {
                if (netNode instanceof AccessorNode acc
                        && !acc.getBlockPos().equals(targetPos)
                        && (targetContainer == null || !Objects.equals(targetContainer, acc.getContainerPos()))) {
                    int available = acc.countMatching(filter, level);
                    if (available > bestAvailable) {
                        bestAvailable = available;
                        sourceNode = acc;
                        if (available >= quantity) break;
                    }
                }
            }
        }

        if (sourceNode == null) {
            log(log, level, computerPos, ComputerLogEntry.Severity.WARN,
                    String.format("Transfer failed: no %s available on the target's item network.",
                            filter.describe()));
            return Result.FAILED;
        }
        ItemNetwork sourceNetwork = targetNetwork;

        int available = sourceNode.countMatching(filter, level);
        int toSend = allowPartial ? Math.min(quantity, available) : quantity;
        if (toSend <= 0 || available < toSend) {
            log(log, level, computerPos, ComputerLogEntry.Severity.WARN,
                    String.format("Transfer failed: source %s only has %dx %s (requested %d).",
                            sourceNode.getBlockPos().toShortString(), available, filter.describe(), quantity));
            return Result.FAILED;
        }

        // ── Energy gate: no parcels unless there is enough for the whole transfer ──
        long requirement = (long) toSend * Parcel.ENERGY_PRICE_PER_ITEM;
        long energy = energyNetwork.requestEnergyConsumption(requirement);
        if (energy < requirement) {
            energyNetwork.reportEnergyProduction(energy);
            log(log, level, computerPos, ComputerLogEntry.Severity.WARN,
                    String.format("Transfer failed: not enough energy. Needed %d, found %d.", requirement, energy));
            return Result.FAILED;
        }

        // ── Atomic extract + ship ──
        List<ItemStack> extracted = sourceNode.extractMatching(filter, toSend, level, false);
        if (extracted.isEmpty()) {
            energyNetwork.reportEnergyProduction(energy);
            log(log, level, computerPos, ComputerLogEntry.Severity.WARN,
                    String.format("Transfer failed: could not extract %dx %s from source %s.",
                            toSend, filter.describe(), sourceNode.getBlockPos().toShortString()));
            return Result.FAILED;
        }

        int parcelCount = sourceNetwork.requestTransfer(extracted, sourceNode.getBlockPos(), targetPos);

        log(log, level, computerPos, ComputerLogEntry.Severity.INFO,
                String.format("Dispatched %dx %s from %s to %s (%d parcel%s).",
                        toSend, filter.describe(),
                        sourceNode.getBlockPos().toShortString(), targetPos.toShortString(),
                        parcelCount, parcelCount == 1 ? "" : "s"));

        return new Result(true, toSend, parcelCount);
    }

    private static void log(boolean enabled, ServerLevel level, BlockPos computerPos,
                            ComputerLogEntry.Severity severity, String message) {
        if (enabled) {
            ComputerLogger.log(level, computerPos, severity, message);
        }
    }
}

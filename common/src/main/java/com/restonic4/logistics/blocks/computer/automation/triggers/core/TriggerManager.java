package com.restonic4.logistics.blocks.computer.automation.triggers.core;

import com.restonic4.logistics.blocks.computer.ComputerNode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The automation engine owned by a {@link ComputerNode}.
 * <p>
 * Holds the user-configured {@link Trigger} list plus every currently running
 * {@link ActionSequenceTracker}. Driven once per game tick from
 * {@code ComputerNode#tickTriggers()} on the server tick loop — all "concurrency"
 * between sequences is purely logical interleaving across ticks; no threads are involved.
 */
public final class TriggerManager {
    private static final String TAG_TRIGGERS = "triggers";
    private static final String TAG_ACTIVE_SEQUENCES = "activeSequences";

    private final ComputerNode owner;
    private final List<Trigger> triggers = new ArrayList<>();
    private final List<ActionSequenceTracker> activeSequences = new ArrayList<>();

    public TriggerManager(ComputerNode owner) {
        this.owner = owner;
    }

    /** The computer node this manager belongs to. */
    public ComputerNode getOwner() { return owner; }

    // Configuration API (backing the data-only UI)

    /** The user-configured triggers, in evaluation order. */
    public List<Trigger> getTriggers() { return Collections.unmodifiableList(triggers); }

    public void addTrigger(Trigger trigger) {
        triggers.add(trigger);
    }

    /** Removes a trigger and aborts any of its still-running action sequences. */
    public void removeTrigger(Trigger trigger) {
        triggers.remove(trigger);
        activeSequences.removeIf(tracker -> tracker.getTrigger() == trigger);
    }

    /**
     * Replaces the whole trigger configuration (e.g. when the user saves the Triggers tab).
     * <p>
     * Running action sequences are preserved when their trigger still exists (matched by
     * instance ID) with an identical action list — so editing one trigger doesn't abort an
     * unrelated trigger's in-flight delay. Sequences of removed or action-modified triggers
     * are dropped, since their position/state may no longer line up.
     */
    public void replaceTriggers(List<Trigger> newTriggers) {
        Map<UUID, Trigger> replacements = new HashMap<>();
        for (Trigger trigger : newTriggers) {
            replacements.put(trigger.getInstanceId(), trigger);
        }

        // The edge-detection state never travels to the client, so carry it over from the
        // old instances — otherwise saving would re-fire ONCE_UNTIL_FALSE triggers whose
        // condition is currently true.
        for (Trigger old : triggers) {
            Trigger replacement = replacements.get(old.getInstanceId());
            if (replacement != null) {
                replacement.setWasActiveLastTick(old.wasActiveLastTick());
            }
        }

        List<ActionSequenceTracker> kept = new ArrayList<>();
        for (ActionSequenceTracker tracker : activeSequences) {
            Trigger replacement = replacements.get(tracker.getTrigger().getInstanceId());
            if (replacement != null
                    && actionListsMatch(tracker.getTrigger(), replacement)
                    && tracker.getActionIndex() < replacement.getActions().size()) {
                kept.add(tracker.retarget(replacement));
            }
        }

        triggers.clear();
        triggers.addAll(newTriggers);
        activeSequences.clear();
        activeSequences.addAll(kept);
    }

    private static boolean actionListsMatch(Trigger a, Trigger b) {
        List<TriggerAction> actionsA = a.getActions();
        List<TriggerAction> actionsB = b.getActions();
        if (actionsA.size() != actionsB.size()) return false;
        for (int i = 0; i < actionsA.size(); i++) {
            if (!actionsA.get(i).save().equals(actionsB.get(i).save())) return false;
        }
        return true;
    }

    /** The currently running action sequences (running delays included). */
    public List<ActionSequenceTracker> getActiveSequences() { return Collections.unmodifiableList(activeSequences); }

    /** Whether the given trigger has at least one action sequence still running. */
    public boolean hasActiveSequence(Trigger trigger) {
        for (ActionSequenceTracker tracker : activeSequences) {
            if (tracker.getTrigger() == trigger) return true;
        }
        return false;
    }

    // Tick engine

    /**
     * Runs one tick of the automation engine:
     * <ol>
     *   <li>Evaluates every trigger against the snapshot and, depending on its
     *       {@link Trigger.ExecutionMode} and overlap setting, spawns new sequences.</li>
     *   <li>Advances every active sequence until it holds, fails or completes,
     *       removing finished ones.</li>
     * </ol>
     */
    public void tick(TriggerContext ctx) {
        for (Trigger trigger : triggers) {
            boolean active = trigger.evaluate(ctx);

            boolean shouldFire = switch (trigger.getMode()) {
                case CONTINUOUS -> active;
                case ONCE_UNTIL_FALSE -> active && !trigger.wasActiveLastTick();
            };
            trigger.setWasActiveLastTick(active);

            if (shouldFire
                    && !trigger.getActions().isEmpty()
                    && (trigger.allowsOverlap() || !hasActiveSequence(trigger))) {
                activeSequences.add(new ActionSequenceTracker(trigger));
            }
        }

        Iterator<ActionSequenceTracker> iterator = activeSequences.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick(ctx)) {
                iterator.remove();
            }
        }
    }

    // Disk serialization (NBT)

    /** Serializes all configured triggers and every in-flight sequence (so delays persist through restarts). */
    public void saveExtra(CompoundTag tag) {
        ListTag triggerList = new ListTag();
        for (Trigger trigger : triggers) {
            triggerList.add(trigger.save());
        }
        tag.put(TAG_TRIGGERS, triggerList);

        ListTag sequenceList = new ListTag();
        for (ActionSequenceTracker tracker : activeSequences) {
            sequenceList.add(tracker.save());
        }
        tag.put(TAG_ACTIVE_SEQUENCES, sequenceList);
    }

    /** Restores triggers first, then re-links every persisted sequence to its trigger. */
    public void loadExtra(CompoundTag tag) {
        triggers.clear();
        ListTag triggerList = tag.getList(TAG_TRIGGERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < triggerList.size(); i++) {
            triggers.add(Trigger.createFromTag(triggerList.getCompound(i)));
        }

        activeSequences.clear();
        ListTag sequenceList = tag.getList(TAG_ACTIVE_SEQUENCES, Tag.TAG_COMPOUND);
        for (int i = 0; i < sequenceList.size(); i++) {
            ActionSequenceTracker tracker = ActionSequenceTracker.createFromTag(sequenceList.getCompound(i), triggers);
            if (tracker != null) {
                activeSequences.add(tracker);
            }
        }
    }

    // Network serialization

    /** Serializes triggers and running sequence positions for client display. */
    public void writeExtraSyncData(FriendlyByteBuf buf) {
        buf.writeVarInt(triggers.size());
        for (Trigger trigger : triggers) {
            trigger.writeSyncData(buf);
        }

        buf.writeVarInt(activeSequences.size());
        for (ActionSequenceTracker tracker : activeSequences) {
            tracker.writeSyncData(buf);
        }
    }

    /** Restores triggers first, then re-links every synced sequence to its trigger. */
    public void readExtraSyncData(FriendlyByteBuf buf) {
        triggers.clear();
        int triggerCount = buf.readVarInt();
        for (int i = 0; i < triggerCount; i++) {
            triggers.add(Trigger.createFromBuf(buf));
        }

        activeSequences.clear();
        int sequenceCount = buf.readVarInt();
        for (int i = 0; i < sequenceCount; i++) {
            ActionSequenceTracker tracker = ActionSequenceTracker.createFromBuf(buf, triggers);
            if (tracker != null) {
                activeSequences.add(tracker);
            }
        }
    }
}

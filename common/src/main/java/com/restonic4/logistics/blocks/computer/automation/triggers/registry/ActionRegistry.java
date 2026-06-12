package com.restonic4.logistics.blocks.computer.automation.triggers.registry;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.blocks.computer.automation.triggers.actions.LogMessageAction;
import com.restonic4.logistics.blocks.computer.automation.triggers.actions.PlayAudioAction;
import com.restonic4.logistics.blocks.computer.automation.triggers.actions.SendItemsAction;
import com.restonic4.logistics.blocks.computer.automation.triggers.actions.StopAudioAction;
import com.restonic4.logistics.blocks.computer.automation.triggers.actions.WaitAudioAction;
import com.restonic4.logistics.blocks.computer.automation.triggers.actions.WaitTicksAction;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static registry of every available {@link ActionType}. Action configurations are
 * serialized by type ID, so anything written to disk or network must be registered here
 * before it can be reconstructed.
 */
public final class ActionRegistry {
    private static final Map<ResourceLocation, ActionType<?>> REGISTRY = new LinkedHashMap<>();

    // Built-in action types
    public static final ActionType<WaitTicksAction> WAIT_TICKS = register(
            new ActionType<>(new ResourceLocation(Constants.MOD_ID, "wait_ticks"), WaitTicksAction::new));
    public static final ActionType<PlayAudioAction> PLAY_AUDIO = register(
            new ActionType<>(new ResourceLocation(Constants.MOD_ID, "play_audio"), PlayAudioAction::new));
    public static final ActionType<StopAudioAction> STOP_AUDIO = register(
            new ActionType<>(new ResourceLocation(Constants.MOD_ID, "stop_audio"), StopAudioAction::new));
    public static final ActionType<WaitAudioAction> WAIT_AUDIO = register(
            new ActionType<>(new ResourceLocation(Constants.MOD_ID, "wait_audio"), WaitAudioAction::new));
    public static final ActionType<LogMessageAction> LOG_MESSAGE = register(
            new ActionType<>(new ResourceLocation(Constants.MOD_ID, "log_message"), LogMessageAction::new));
    public static final ActionType<SendItemsAction> SEND_ITEMS = register(
            new ActionType<>(new ResourceLocation(Constants.MOD_ID, "send_items"), SendItemsAction::new));

    private ActionRegistry() {}

    /**
     * Registers an action type. Intended to be called once per type during static initialization.
     *
     * @throws IllegalStateException if the ID is already taken
     */
    public static <A extends TriggerAction> ActionType<A> register(ActionType<A> type) {
        if (REGISTRY.putIfAbsent(type.getId(), type) != null) {
            throw new IllegalStateException("Duplicate action type registration: " + type.getId());
        }
        return type;
    }

    /** Looks up an action type by its ID, or {@code null} if unknown. */
    public static ActionType<?> get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    /**
     * Looks up an action type by its ID for deserialization.
     *
     * @throws IllegalArgumentException if the ID is unknown
     */
    public static ActionType<?> getOrThrow(ResourceLocation id) {
        ActionType<?> type = REGISTRY.get(id);
        if (type == null) {
            throw new IllegalArgumentException("Unknown action type: " + id);
        }
        return type;
    }

    /** All registered action types, in registration order (e.g. for UI listings). */
    public static Collection<ActionType<?>> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }
}

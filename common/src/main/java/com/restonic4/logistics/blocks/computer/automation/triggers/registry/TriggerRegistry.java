package com.restonic4.logistics.blocks.computer.automation.triggers.registry;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.Trigger;
import com.restonic4.logistics.blocks.computer.automation.triggers.types.AudioStateTrigger;
import com.restonic4.logistics.blocks.computer.automation.triggers.types.EnergyLevelTrigger;
import com.restonic4.logistics.blocks.computer.automation.triggers.types.IntervalTrigger;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static registry of every available {@link TriggerType}. Trigger configurations are
 * serialized by type ID, so anything written to disk or network must be registered here
 * before it can be reconstructed.
 */
public final class TriggerRegistry {
    private static final Map<ResourceLocation, TriggerType<?>> REGISTRY = new LinkedHashMap<>();

    // Built-in trigger types
    public static final TriggerType<EnergyLevelTrigger> ENERGY_LEVEL = register(
            new TriggerType<>(new ResourceLocation(Constants.MOD_ID, "energy_level"), EnergyLevelTrigger::new));
    public static final TriggerType<AudioStateTrigger> AUDIO_STATE = register(
            new TriggerType<>(new ResourceLocation(Constants.MOD_ID, "audio_state"), AudioStateTrigger::new));
    public static final TriggerType<IntervalTrigger> INTERVAL = register(
            new TriggerType<>(new ResourceLocation(Constants.MOD_ID, "interval"), IntervalTrigger::new));

    private TriggerRegistry() {}

    /**
     * Registers a trigger type. Intended to be called once per type during static initialization.
     *
     * @throws IllegalStateException if the ID is already taken
     */
    public static <T extends Trigger> TriggerType<T> register(TriggerType<T> type) {
        if (REGISTRY.putIfAbsent(type.getId(), type) != null) {
            throw new IllegalStateException("Duplicate trigger type registration: " + type.getId());
        }
        return type;
    }

    /** Looks up a trigger type by its ID, or {@code null} if unknown. */
    public static TriggerType<?> get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    /**
     * Looks up a trigger type by its ID for deserialization.
     *
     * @throws IllegalArgumentException if the ID is unknown
     */
    public static TriggerType<?> getOrThrow(ResourceLocation id) {
        TriggerType<?> type = REGISTRY.get(id);
        if (type == null) {
            throw new IllegalArgumentException("Unknown trigger type: " + id);
        }
        return type;
    }

    /** All registered trigger types, in registration order (e.g. for UI listings). */
    public static Collection<TriggerType<?>> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }
}

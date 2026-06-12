package com.restonic4.logistics.blocks.computer.screen.triggers.editors;

import com.restonic4.logistics.blocks.computer.automation.triggers.core.Trigger;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.TriggerRegistry;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.TriggerType;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.types.AudioStateTriggerEditor;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.types.EnergyLevelTriggerEditor;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.types.IntervalTriggerEditor;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.types.ItemCountTriggerEditor;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side registry pairing each {@link TriggerType} with its {@link TriggerEditor}.
 * This is the only place the trigger UI dispatches on type, so adding a trigger means
 * registering its editor here — never touching the tab. Unregistered triggers degrade
 * gracefully: a generic summary and no extra config rows.
 */
public final class TriggerEditors {
    private static final Map<TriggerType<?>, TriggerEditor<?>> REGISTRY = new HashMap<>();

    static {
        register(TriggerRegistry.ENERGY_LEVEL, new EnergyLevelTriggerEditor());
        register(TriggerRegistry.AUDIO_STATE, new AudioStateTriggerEditor());
        register(TriggerRegistry.INTERVAL, new IntervalTriggerEditor());
        register(TriggerRegistry.ITEM_COUNT, new ItemCountTriggerEditor());
    }

    private TriggerEditors() {}

    public static <T extends Trigger> void register(TriggerType<T> type, TriggerEditor<T> editor) {
        if (REGISTRY.putIfAbsent(type, editor) != null) {
            throw new IllegalStateException("Duplicate trigger editor registration: " + type.getId());
        }
    }

    /** The list summary for a trigger, falling back to its type path if no editor is registered. */
    @SuppressWarnings("unchecked")
    public static String summary(Trigger trigger) {
        TriggerEditor<Trigger> editor = (TriggerEditor<Trigger>) REGISTRY.get(trigger.getType());
        if (editor == null) return trigger.getType().getId().getPath();
        return editor.summary(trigger);
    }

    /** Lays out a trigger's type-specific config rows; a no-op for unregistered triggers. */
    @SuppressWarnings("unchecked")
    public static void buildConfig(Trigger trigger, EditorBuilder builder) {
        TriggerEditor<Trigger> editor = (TriggerEditor<Trigger>) REGISTRY.get(trigger.getType());
        if (editor != null) editor.buildConfig(trigger, builder);
    }
}

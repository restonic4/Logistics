package com.restonic4.logistics.blocks.computer.screen.triggers.editors;

import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerAction;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.ActionRegistry;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.ActionType;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.actions.LogMessageActionEditor;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.actions.PlayAudioActionEditor;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.actions.StopAudioActionEditor;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.actions.WaitAudioActionEditor;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.actions.WaitTicksActionEditor;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side registry pairing each {@link ActionType} with its {@link ActionEditor}.
 * This is the only place the action UI dispatches on type, so adding an action means
 * registering its editor here — never touching the tab. Unregistered actions degrade
 * gracefully: their header still renders, just with no extra config rows.
 */
public final class ActionEditors {
    private static final Map<ActionType<?>, ActionEditor<?>> REGISTRY = new HashMap<>();

    static {
        register(ActionRegistry.WAIT_TICKS, new WaitTicksActionEditor());
        register(ActionRegistry.PLAY_AUDIO, new PlayAudioActionEditor());
        register(ActionRegistry.STOP_AUDIO, new StopAudioActionEditor());
        register(ActionRegistry.WAIT_AUDIO, new WaitAudioActionEditor());
        register(ActionRegistry.LOG_MESSAGE, new LogMessageActionEditor());
    }

    private ActionEditors() {}

    public static <A extends TriggerAction> void register(ActionType<A> type, ActionEditor<A> editor) {
        if (REGISTRY.putIfAbsent(type, editor) != null) {
            throw new IllegalStateException("Duplicate action editor registration: " + type.getId());
        }
    }

    /** Lays out an action's type-specific config rows; a no-op for unregistered actions. */
    @SuppressWarnings("unchecked")
    public static void buildConfig(TriggerAction action, EditorBuilder builder) {
        ActionEditor<TriggerAction> editor = (ActionEditor<TriggerAction>) REGISTRY.get(action.getType());
        if (editor != null) editor.buildConfig(action, builder);
    }
}

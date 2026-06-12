package com.restonic4.logistics.blocks.computer.screen.triggers.editors;

import com.restonic4.logistics.blocks.computer.automation.triggers.core.Trigger;

/**
 * The client-side presentation for one kind of {@link Trigger}: the one-line summary shown
 * in the trigger list, and the configuration rows shown when it is selected. Kept separate
 * from the trigger class itself so the server-side logic stays free of GUI code; registered
 * against its {@link com.restonic4.logistics.blocks.computer.automation.triggers.registry.TriggerType}
 * in {@link TriggerEditors}.
 *
 * @param <T> the concrete trigger this editor configures
 */
public interface TriggerEditor<T extends Trigger> {
    /** The detail text for the trigger list entry (the action count is appended by the tab). */
    String summary(T trigger);

    /** Lays out this trigger's type-specific configuration rows using {@code builder}. */
    void buildConfig(T trigger, EditorBuilder builder);
}

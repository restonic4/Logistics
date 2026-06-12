package com.restonic4.logistics.blocks.computer.screen.triggers.editors;

import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerAction;

/**
 * The client-side presentation for one kind of {@link TriggerAction}: the configuration rows
 * shown beneath its header in a trigger's action list. Actions have no list summary (the tab
 * renders their type name), so this is config-only. Kept separate from the action class so
 * the server-side logic stays free of GUI code; registered against its
 * {@link com.restonic4.logistics.blocks.computer.automation.triggers.registry.ActionType}
 * in {@link ActionEditors}.
 *
 * @param <A> the concrete action this editor configures
 */
public interface ActionEditor<A extends TriggerAction> {
    /** Lays out this action's type-specific configuration rows using {@code builder}. */
    void buildConfig(A action, EditorBuilder builder);
}

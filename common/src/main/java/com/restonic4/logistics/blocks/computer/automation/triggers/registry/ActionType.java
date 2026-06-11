package com.restonic4.logistics.blocks.computer.automation.triggers.registry;

import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerAction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * A registered action kind: a unique {@link ResourceLocation} identifier paired with a
 * factory that creates fresh, default-configured instances. Used by {@link ActionRegistry}
 * to reconstruct the correct {@link TriggerAction} subclass during deserialization.
 *
 * @param <A> the concrete action class this type produces
 */
public final class ActionType<A extends TriggerAction> {
    private final ResourceLocation id;
    private final Supplier<A> factory;

    public ActionType(ResourceLocation id, Supplier<A> factory) {
        this.id = id;
        this.factory = factory;
    }

    /** The unique identifier of this action type, written to disk and network. */
    public ResourceLocation getId() { return id; }

    /** Creates a new action instance with default configuration. */
    public A create() { return factory.get(); }

    /** Human-readable name (lang key {@code action.<namespace>.<path>}), used by UIs. */
    public Component getDisplayName() {
        return Component.translatable("action." + id.getNamespace() + "." + id.getPath());
    }
}

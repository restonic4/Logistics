package com.restonic4.logistics.blocks.computer.automation.triggers.registry;

import com.restonic4.logistics.blocks.computer.automation.triggers.core.Trigger;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * A registered trigger kind: a unique {@link ResourceLocation} identifier paired with a
 * factory that creates fresh, default-configured instances. Used by {@link TriggerRegistry}
 * to reconstruct the correct {@link Trigger} subclass during deserialization.
 *
 * @param <T> the concrete trigger class this type produces
 */
public final class TriggerType<T extends Trigger> {
    private final ResourceLocation id;
    private final Supplier<T> factory;

    public TriggerType(ResourceLocation id, Supplier<T> factory) {
        this.id = id;
        this.factory = factory;
    }

    /** The unique identifier of this trigger type, written to disk and network. */
    public ResourceLocation getId() { return id; }

    /** Creates a new trigger instance with default configuration. */
    public T create() { return factory.get(); }

    /** Human-readable name (lang key {@code trigger.<namespace>.<path>}), used by UIs. */
    public Component getDisplayName() {
        return Component.translatable("trigger." + id.getNamespace() + "." + id.getPath());
    }
}

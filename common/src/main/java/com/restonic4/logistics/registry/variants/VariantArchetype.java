package com.restonic4.logistics.registry.variants;

import com.restonic4.logistics.registry.variants.archetypes.CubeAllArchetype;
import com.restonic4.logistics.registry.variants.archetypes.LitArchetype;

/**
 * A reusable template that knows how to generate the client assets (blockstate, models, item
 * model) for a family of variant blocks. Implementations are passed their parameters (textures,
 * etc.) at construction and emit resources through the {@link GenerationContext}.
 *
 * <p>Add a new archetype by implementing this interface (see
 * {@link CubeAllArchetype} and
 * {@link LitArchetype}); register a block against it
 * with {@code BlockBuilder#variant(...)}.
 */
@FunctionalInterface
public interface VariantArchetype {
    void emit(GenerationContext ctx);
}

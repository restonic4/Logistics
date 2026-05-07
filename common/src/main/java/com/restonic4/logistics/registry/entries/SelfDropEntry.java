package com.restonic4.logistics.registry.entries;

import net.minecraft.resources.ResourceLocation;

public record SelfDropEntry(ResourceLocation blockId, boolean survivesExplosion) {}

package com.restonic4.logistics.compatibility;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;

public interface CreateCompatibility extends Compatibility {
    boolean hasGoggleOverlay(ServerLevel level, BlockPos pos);
}

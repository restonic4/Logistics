package com.restonic4.logistics.platform;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.platform.services.PlatformHelper;
import com.restonic4.logistics.platform.services.TargetedPlatformRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ServiceLoader;

public class Services {
    public static final PlatformHelper PLATFORM = load(PlatformHelper.class);
    @SuppressWarnings("unchecked")
    public static final TargetedPlatformRegistry<Block, BlockEntity, Item, NetworkNode> PLATFORM_REGISTRY = (TargetedPlatformRegistry<Block, BlockEntity, Item, NetworkNode>) load(TargetedPlatformRegistry.class);

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz).findFirst().orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
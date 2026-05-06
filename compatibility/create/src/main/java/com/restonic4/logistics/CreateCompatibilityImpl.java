package com.restonic4.logistics;

import com.restonic4.logistics.compatibility.CreateCompatibility;
import com.restonic4.logistics.platform.Services;
import com.restonic4.logistics.platform.services.PlatformHelper;
import com.simibubi.create.AllCreativeModeTabs;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;

import java.lang.reflect.InvocationTargetException;

public class CreateCompatibilityImpl implements CreateCompatibility {
    @Override
    public void registerCommon() {
        CreateCommonCompatibility.register();
    }

    @Override
    public void registerClient() {
        CreateClientCompatibility.register();
    }

    @Override
    public void registerServer() {

    }

    @Override
    public boolean hasGoggleOverlay(ServerLevel level, BlockPos pos) {
        return CreateCommonCompatibility.hasGoggleOverlay(level, pos);
    }

    static ResourceKey<CreativeModeTab> CREATE_TAB = null;
    public static ResourceKey<CreativeModeTab> getBaseCreativeTab() {
        if (CREATE_TAB == null) {
            try {
                if (Services.PLATFORM.isFabric()) {
                    var method = AllCreativeModeTabs.BASE_CREATIVE_TAB.getClass().getMethod("key");
                    CREATE_TAB = (ResourceKey<CreativeModeTab>) method.invoke(AllCreativeModeTabs.BASE_CREATIVE_TAB);
                } else if (Services.PLATFORM.isForge()) {
                    var method = AllCreativeModeTabs.BASE_CREATIVE_TAB.getClass().getMethod("getKey");
                    CREATE_TAB =  (ResourceKey<CreativeModeTab>) method.invoke(AllCreativeModeTabs.BASE_CREATIVE_TAB);
                } else {
                    CREATE_TAB = null;
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not find method to extract the tab key");
            }
        }

        return CREATE_TAB;
    }
}

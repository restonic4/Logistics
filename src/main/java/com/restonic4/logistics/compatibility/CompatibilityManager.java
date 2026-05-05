package com.restonic4.logistics.compatibility;

import com.restonic4.logistics.compatibility.create.CreateClientCompatibility;
import com.restonic4.logistics.compatibility.create.CreateCompatibility;
import net.fabricmc.loader.api.FabricLoader;

public class CompatibilityManager {
    public static void register() {
        if (isCreateLoaded()) {
            CreateCompatibility.register();
        }
    }

    public static void registerClient() {
        if (isCreateLoaded()) {
            CreateClientCompatibility.register();
        }
    }

    public static boolean isCreateLoaded() {
        return FabricLoader.getInstance().isModLoaded("create");
    }
}

package com.restonic4.logistics.compatibility;

import com.restonic4.logistics.compatibility.create.CreateClientCompatibility;
import com.restonic4.logistics.compatibility.create.CreateCompatibility;
import net.fabricmc.loader.api.FabricLoader;

public class CompatibilityManager {
    public static void register() {
        FabricLoader fabricLoader = FabricLoader.getInstance();

        if (fabricLoader.isModLoaded("create")) {
            CreateCompatibility.register();
        }
    }

    public static void registerClient() {
        FabricLoader fabricLoader = FabricLoader.getInstance();

        if (fabricLoader.isModLoaded("create")) {
            CreateClientCompatibility.register();
        }
    }
}

package com.restonic4.logistics.compatibility;

//import com.restonic4.logistics.compatibility.create.CreateClientCompatibility;
//import com.restonic4.logistics.compatibility.create.CreateCompatibility;
import com.restonic4.logistics.platform.Services;

public class CompatibilityManager {
    public static void register() {
        if (isCreateLoaded()) {
            //CreateCompatibility.register();
        }
    }

    public static void registerClient() {
        if (isCreateLoaded()) {
            //CreateClientCompatibility.register();
        }
    }

    public static boolean isCreateLoaded() {
        return Services.PLATFORM.isModLoaded("create");
    }
}

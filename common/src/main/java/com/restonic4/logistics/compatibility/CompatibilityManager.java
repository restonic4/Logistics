package com.restonic4.logistics.compatibility;

import com.restonic4.logistics.platform.Services;

import java.util.ServiceLoader;

public class CompatibilityManager {
    private static CreateCompatibility CREATE = null;

    public static void registerCommon() {
        if (isCreateLoaded()) {
            getCreateCompatibilityLayer().registerCommon();
        }
    }

    public static void registerClient() {
        if (isCreateLoaded()) {
            getCreateCompatibilityLayer().registerClient();
        }
    }

    public static void registerServer() {
        if (isCreateLoaded()) {
            getCreateCompatibilityLayer().registerServer();
        }
    }

    public static CreateCompatibility getCreateCompatibilityLayer() {
        if (CREATE == null) {
            CREATE = ServiceLoader.load(CreateCompatibility.class)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No CreateCompatibility implementation found"));
        }
        return CREATE;
    }

    public static boolean isCreateLoaded() {
        return Services.PLATFORM.isModLoaded("create");
    }
}

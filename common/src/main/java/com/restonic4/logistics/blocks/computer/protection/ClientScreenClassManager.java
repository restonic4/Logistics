package com.restonic4.logistics.blocks.computer.protection;

import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import net.minecraft.client.Minecraft;

public class ClientScreenClassManager {
    public static void openScreen(Minecraft client, ProtectionEditSyncPacket packet) {
        client.execute(() -> {
            if (client.screen instanceof ComputerScreen screen) {
                screen.setProtectionData(packet);
            }
        });
    }
}

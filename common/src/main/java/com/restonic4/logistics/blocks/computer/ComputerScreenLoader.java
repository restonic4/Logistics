package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import net.minecraft.client.Minecraft;

public class ComputerScreenLoader {
    public static void open(Minecraft client, ComputerSyncPacket packet) {
        ComputerScreen.setAccessors(packet);
        ComputerScreen.setComputerState(packet);
        client.setScreen(new ComputerScreen());
    }
}

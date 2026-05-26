package com.restonic4.logistics.blocks.charging_station;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class ChargingStationScreenDispatcher {
    public static void open(BlockPos pos) {
        Minecraft.getInstance().setScreen(new ChargingStationScreen(pos));
    }
}

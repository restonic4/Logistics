package com.restonic4.logistics;

import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class LogisticsForge {
    public LogisticsForge() {
        Logistics.init();
    }
}
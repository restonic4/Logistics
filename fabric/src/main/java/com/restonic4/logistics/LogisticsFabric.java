package com.restonic4.logistics;

import com.restonic4.logistics.experiment.ShockwaveCommand;
import com.restonic4.logistics.registry.PlatformRegistry;
import com.restonic4.logistics.voicechat.WalkieTalkieItem;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public class LogisticsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Logistics.init();
        //Registry.register(BuiltInRegistries.ITEM, Logistics.id("walkie"), new WalkieTalkieItem(new Item.Properties()));
        PlatformRegistry.freeze();
        ShockwaveCommand.init();
    }
}
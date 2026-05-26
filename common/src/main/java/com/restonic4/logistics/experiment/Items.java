package com.restonic4.logistics.experiment;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.registry.PlatformRegistry;
import com.restonic4.logistics.registry.entries.ItemEntry;
import net.minecraft.world.item.Item;

public class Items {
    public static final ItemEntry SILICON = PlatformRegistry
            .item(
                Logistics.id("silicon"),
                () -> new Item(new Item.Properties())
            )
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final ItemEntry CHIP = PlatformRegistry
            .item(
                    Logistics.id("chip"),
                    () -> new Item(new Item.Properties())
            )
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final ItemEntry CRYSTAL_SHARD = PlatformRegistry
            .item(
                    Logistics.id("crystal_shard"),
                    () -> new CrystalShardItem(new Item.Properties())
            )
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static void register() {

    }
}

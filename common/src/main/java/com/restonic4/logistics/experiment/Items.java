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

    public static final ItemEntry KINETIC_CRYSTAL_SHARD = PlatformRegistry
            .item(
                    Logistics.id("kinetic_crystal_shard"),
                    () -> new KineticCrystalShardItem(new Item.Properties())
            )
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static final ItemEntry BROKEN_KINETIC_CRYSTAL_SHARD = PlatformRegistry
            .item(
                    Logistics.id("broken_kinetic_crystal_shard"),
                    () -> new BrokenKineticCrystalShardItem(new Item.Properties())
            )
            .addToTab(Logistics.CUSTOM_TAB.getKey())
            .register();

    public static void register() {

    }
}

package com.restonic4.logistics;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.compatibility.CompatibilityManager;
import com.restonic4.logistics.events.ServerTickEvents;
import com.restonic4.logistics.networking.NetworkTooltipPayload;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.tooltip.NetworkScannerServerHandler;
import com.restonic4.logistics.registry.CreativeTabEntry;
import com.restonic4.logistics.registry.Registrate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class Logistics {
    public static final CreativeTabEntry CUSTOM_TAB = Registrate
            .tab(
                    id("custom_tab"),
                    () -> new ItemStack(BlockRegistry.BATTERY_BLOCK.getItem())
            )
            .register();

    public static void init() {
        BlockRegistry.register();
        CompatibilityManager.register();
        NetworkManager.register();
        NetworkTooltipPayload.register();
        Registrate.build();

        ServerTickEvents.END.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                NetworkScannerServerHandler.tick(p);
            }
        });
    }

    public static ResourceLocation id(String id) { return new ResourceLocation(Constants.MOD_ID, id); }
}

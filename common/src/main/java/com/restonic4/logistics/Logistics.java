package com.restonic4.logistics;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.accersor.AccessorBlock;
import com.restonic4.logistics.blocks.computer.ComputerTransferPacket;
import com.restonic4.logistics.compatibility.CompatibilityManager;
import com.restonic4.logistics.events.ChunkEvents;
import com.restonic4.logistics.events.ServerTickEvents;
import com.restonic4.logistics.networking.NetworkingRegistry;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.tooltip.NetworkScannerServerHandler;
import com.restonic4.logistics.registry.PlatformRegistry;
import com.restonic4.logistics.registry.entries.CreativeTabEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class Logistics {
    public static final CreativeTabEntry CUSTOM_TAB = PlatformRegistry
            .tab(
                    id("custom_tab"),
                    () -> new ItemStack(BlockRegistry.BATTERY_BLOCK.getItem())
            )
            .register();

    public static void init() {
        BlockRegistry.register();
        CompatibilityManager.registerCommon();
        NetworkManager.register();

        ServerTickEvents.END.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                NetworkScannerServerHandler.tick(p);
            }
        });

        AccessorBlock.registerEvents();

        NetworkingRegistry.registerServerTargetedPacket(ComputerTransferPacket.ID, ComputerTransferPacket::new);
    }

    public static ResourceLocation id(String id) { return new ResourceLocation(Constants.MOD_ID, id); }
}

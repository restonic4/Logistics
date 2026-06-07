package com.restonic4.logistics;

import com.restonic4.logistics.audio.network.ServerboundAudioControlPacket;
import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.accersor.AccessorBlock;
import com.restonic4.logistics.blocks.computer.ComputerClientLogPushPacket;
import com.restonic4.logistics.blocks.computer.ComputerInstallPacket;
import com.restonic4.logistics.blocks.computer.ComputerScreenOffPacket;
import com.restonic4.logistics.blocks.computer.ComputerTransferPacket;
import com.restonic4.logistics.blocks.computer.protection.ProtectionSavePacket;
import com.restonic4.logistics.blocks.protector.data_types.FlagRegistry;
import com.restonic4.logistics.compatibility.CompatibilityManager;
import com.restonic4.logistics.events.ServerTickEvents;
import com.restonic4.logistics.experiment.Items;
import com.restonic4.logistics.experiment.Recipes;
import com.restonic4.logistics.experiment.Sounds;
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
                    () -> new ItemStack(BlockRegistry.COMPUTER_BLOCK.getItem())
            )
            .register();

    public static void init() {
        FlagRegistry.init();
        Items.register();
        BlockRegistry.register();
        Sounds.register();
        CompatibilityManager.registerCommon();
        NetworkManager.register();
        Recipes.register();

        ServerTickEvents.END.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                NetworkScannerServerHandler.tick(p);
            }
        });

        AccessorBlock.registerEvents();

        NetworkingRegistry.registerServerTargetedPacket(ComputerTransferPacket.ID, ComputerTransferPacket::new);
        NetworkingRegistry.registerServerTargetedPacket(ComputerScreenOffPacket.ID, ComputerScreenOffPacket::new);
        NetworkingRegistry.registerServerTargetedPacket(ComputerInstallPacket.ID, ComputerInstallPacket::new);
        NetworkingRegistry.registerServerTargetedPacket(ComputerClientLogPushPacket.ID, ComputerClientLogPushPacket::new);
        NetworkingRegistry.registerServerTargetedPacket(ProtectionSavePacket.ID, ProtectionSavePacket::read);
        NetworkingRegistry.registerServerTargetedPacket(ServerboundAudioControlPacket.ID, ServerboundAudioControlPacket::new);
    }

    public static ResourceLocation id(String id) { return new ResourceLocation(Constants.MOD_ID, id); }
}

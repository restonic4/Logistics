package com.restonic4.logistics;

import com.restonic4.logistics.audio.ServerAudioStorage;
import com.restonic4.logistics.audio.AudioRequestC2SPacket;
import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.accersor.AccessorBlock;
import com.restonic4.logistics.blocks.audio_station.AudioDeletePacket;
import com.restonic4.logistics.blocks.audio_station.AudioStationConfigPacket;
import com.restonic4.logistics.blocks.audio_station.AudioStationControlPacket;
import com.restonic4.logistics.blocks.audio_station.AudioUploadPacket;
import com.restonic4.logistics.blocks.computer.automation.TriggerSavePacket;
import com.restonic4.logistics.blocks.base.RenameNodePacket;
import com.restonic4.logistics.blocks.computer.ComputerClientLogPushPacket;
import com.restonic4.logistics.blocks.computer.ComputerInstallPacket;
import com.restonic4.logistics.blocks.computer.ComputerScreenOffPacket;
import com.restonic4.logistics.blocks.computer.ComputerTransferPacket;
import com.restonic4.logistics.blocks.computer.protection.ProtectionSavePacket;
import com.restonic4.logistics.blocks.protector.data_types.FlagRegistry;
import com.restonic4.logistics.compatibility.CompatibilityManager;
import com.restonic4.logistics.events.ServerTickEvents;
import com.restonic4.logistics.experiment.DebugTogglePacket;
import com.restonic4.logistics.experiment.Items;
import com.restonic4.logistics.experiment.Recipes;
import com.restonic4.logistics.experiment.Sounds;
import com.restonic4.logistics.networking.NetworkingRegistry;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.tooltip.NetworkScannerServerHandler;
import com.restonic4.logistics.platform.Services;
import com.restonic4.logistics.registry.PlatformRegistry;
import com.restonic4.logistics.registry.entries.CreativeTabEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

public class Logistics {
    public static final CreativeTabEntry CUSTOM_TAB = PlatformRegistry
            .tab(
                    id("custom_tab"),
                    () -> new ItemStack(BlockRegistry.COMPUTER_BLOCK.getItem())
            )
            .register();

    public static final CreativeTabEntry DECORATION_TAB = PlatformRegistry
            .tab(
                    id("decoration_tab"),
                    () -> new ItemStack(BlockRegistry.NORMAL_WALLPAPER_BLOCK.getItem())
            )
            .register();

    public static void init() {
        if (Services.PLATFORM.isDevelopmentEnvironment()) Constants.setDebug(true);

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

            if (ServerAudioStorage.getBaseDir() == null) {
                ServerAudioStorage.init(server.getWorldPath(LevelResource.ROOT).toFile());
            }
        });

        AccessorBlock.registerEvents();

        NetworkingRegistry.registerServerTargetedPacket(ComputerTransferPacket.ID, ComputerTransferPacket::new);
        NetworkingRegistry.registerServerTargetedPacket(ComputerScreenOffPacket.ID, ComputerScreenOffPacket::new);
        NetworkingRegistry.registerServerTargetedPacket(ComputerInstallPacket.ID, ComputerInstallPacket::new);
        NetworkingRegistry.registerServerTargetedPacket(ComputerClientLogPushPacket.ID, ComputerClientLogPushPacket::new);
        NetworkingRegistry.registerServerTargetedPacket(ProtectionSavePacket.ID, ProtectionSavePacket::read);
        NetworkingRegistry.registerServerTargetedPacket(AudioStationConfigPacket.ID, AudioStationConfigPacket::new);
        NetworkingRegistry.registerServerTargetedPacket(AudioStationControlPacket.ID, AudioStationControlPacket::new);
        NetworkingRegistry.registerServerTargetedPacket(TriggerSavePacket.ID, TriggerSavePacket::new);
        NetworkingRegistry.registerServerTargetedPacket(AudioUploadPacket.ID, AudioUploadPacket::new);
        NetworkingRegistry.registerServerTargetedPacket(AudioRequestC2SPacket.ID, AudioRequestC2SPacket::new);
        NetworkingRegistry.registerServerTargetedPacket(AudioDeletePacket.ID, AudioDeletePacket::new);
        NetworkingRegistry.registerServerTargetedPacket(RenameNodePacket.ID, RenameNodePacket::new);
        NetworkingRegistry.registerServerTargetedPacket(DebugTogglePacket.ID, DebugTogglePacket::new);
    }

    public static ResourceLocation id(String id) { return new ResourceLocation(Constants.MOD_ID, id); }
}

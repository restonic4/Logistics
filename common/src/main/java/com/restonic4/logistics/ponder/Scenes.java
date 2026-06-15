package com.restonic4.logistics.ponder;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.ponder.scenes.AccessorScene;
import com.restonic4.logistics.ponder.scenes.AudioStationScene;
import com.restonic4.logistics.ponder.scenes.BackroomsScene;
import com.restonic4.logistics.ponder.scenes.BatteryScene;
import com.restonic4.logistics.ponder.scenes.CableScene;
import com.restonic4.logistics.ponder.scenes.ChargingStationScene;
import com.restonic4.logistics.ponder.scenes.ComputerScene;
import com.restonic4.logistics.ponder.scenes.CreateMotorScene;
import com.restonic4.logistics.ponder.scenes.CreateTransformerScene;
import com.restonic4.logistics.ponder.scenes.LampScene;
import com.restonic4.logistics.ponder.scenes.LightSwitchScene;
import com.restonic4.logistics.ponder.scenes.NetworkConnectorScene;
import com.restonic4.logistics.ponder.scenes.NetworkSwitchScene;
import com.restonic4.logistics.ponder.scenes.PipeScene;
import com.restonic4.logistics.ponder.scenes.RedstoneReaderScene;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class Scenes {
    // Every scene animates blocks in code on a checkerboard baseplate structure (scenes/floor_<size>).
    private static final String FLOOR_5 = "scenes/floor_5";
    // The backrooms joke scene loads its own 34x34x34 structure.
    private static final String BACKROOMS = "scenes/backrooms";

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(Logistics.id("protector"), Logistics.id("creative_protector"))
                .addStoryBoard(FLOOR_5, Animations::protector);

        helper.forComponents(Logistics.id("cable"))
                .addStoryBoard(FLOOR_5, CableScene::cable);
        helper.forComponents(Logistics.id("battery"))
                .addStoryBoard(FLOOR_5, BatteryScene::battery);
        helper.forComponents(Logistics.id("network_connector"))
                .addStoryBoard(FLOOR_5, NetworkConnectorScene::networkConnector);
        helper.forComponents(Logistics.id("computer"))
                .addStoryBoard(FLOOR_5, ComputerScene::computer);
        helper.forComponents(Logistics.id("lamp"))
                .addStoryBoard(FLOOR_5, LampScene::lamp);
        helper.forComponents(Logistics.id("charging_station"))
                .addStoryBoard(FLOOR_5, ChargingStationScene::chargingStation);
        helper.forComponents(Logistics.id("audio_station"))
                .addStoryBoard(FLOOR_5, AudioStationScene::audioStation);
        helper.forComponents(Logistics.id("pipe"))
                .addStoryBoard(FLOOR_5, PipeScene::pipe);
        helper.forComponents(Logistics.id("accessor"))
                .addStoryBoard(FLOOR_5, AccessorScene::accessor);
        helper.forComponents(Logistics.id("redstone_reader"))
                .addStoryBoard(FLOOR_5, RedstoneReaderScene::redstoneReader);
        helper.forComponents(Logistics.id("network_switch"))
                .addStoryBoard(FLOOR_5, NetworkSwitchScene::networkSwitch);

        // Worked example: a full redstone light switch. Shown as an extra scene on each block it uses.
        helper.forComponents(
                        Logistics.id("redstone_reader"),
                        Logistics.id("network_switch"),
                        Logistics.id("computer"),
                        Logistics.id("lamp"))
                .addStoryBoard(FLOOR_5, LightSwitchScene::lightSwitch);

        // Backrooms joke scene, shown on every backrooms-themed decoration block.
        helper.forComponents(
                        Logistics.id("normal_wallpaper"),
                        Logistics.id("flat_wallpaper"),
                        Logistics.id("normal_wood_wallpaper"),
                        Logistics.id("flat_wood_wallpaper"),
                        Logistics.id("office_tile"),
                        Logistics.id("office_rug"),
                        Logistics.id("office_lamp"))
                .addStoryBoard(BACKROOMS, BackroomsScene::backrooms);

        // The office lamp is a lamp too: also show the regular lamp scene on it.
        helper.forComponents(Logistics.id("office_lamp"))
                .addStoryBoard(FLOOR_5, LampScene::lamp);

        // Create compatibility blocks: resolved via registry inside the scene, only reachable when Create is present.
        helper.forComponents(Logistics.id("create_motor"))
                .addStoryBoard(FLOOR_5, CreateMotorScene::createMotor);
        helper.forComponents(Logistics.id("create_transformer"))
                .addStoryBoard(FLOOR_5, CreateTransformerScene::createTransformer);
    }
}

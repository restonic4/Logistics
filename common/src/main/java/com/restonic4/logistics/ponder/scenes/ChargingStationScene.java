package com.restonic4.logistics.ponder.scenes;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class ChargingStationScene {
    public static void chargingStation(SceneBuilder scene, SceneBuildingUtil util) {
        PonderHelper p = PonderHelper.of(scene, util);
        p.init("charging_station", 5);

        BlockPos generatorPos = new BlockPos(1, 1, 2);
        BlockPos cablePos = new BlockPos(2, 1, 2);
        BlockPos stationPos = new BlockPos(3, 1, 2);

        BlockState generatorState = BlockRegistry.CREATIVE_GENERATOR_BLOCK.getBlock().defaultBlockState();
        BlockState cableState = BlockRegistry.CABLE_BLOCK.getBlock().defaultBlockState();
        BlockState stationState = BlockRegistry.CHARGING_STATION_BLOCK.getBlock().defaultBlockState();

        p.placeAndShow(stationPos, stationState);
        p.idle(20)
                .textKeyframe("The Charging Station refills energy-storing items.", stationPos, 50)
                .idle(60);

        p.placeAndShow(generatorPos, generatorState);
        p.traceCables(cableState, cablePos)
                .connectStart(Direction.WEST)
                .connectStart(Direction.EAST)
                .showAll(Direction.DOWN);
        p.idle(20)
                .textKeyframe("Power it from the energy network.", stationPos, 50)
                .idle(40);

        p.showInput(stationPos, 60).rightClick().build()
                .textKeyframe("Right-click with a chargeable item to top it up.", stationPos, 50)
                .idle(60);

        scene.markAsFinished();
    }
}

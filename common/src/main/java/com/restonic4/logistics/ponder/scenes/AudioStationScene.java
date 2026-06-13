package com.restonic4.logistics.ponder.scenes;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.computer.ComputerBlock;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class AudioStationScene {
    public static void audioStation(SceneBuilder scene, SceneBuildingUtil util) {
        PonderHelper p = PonderHelper.of(scene, util);
        p.init("audio_station", 5);

        BlockPos computerPos = new BlockPos(1, 1, 2);
        BlockPos cablePos = new BlockPos(2, 1, 2);
        BlockPos audioPos = new BlockPos(3, 1, 2);

        BlockState computerOn = BlockRegistry.COMPUTER_BLOCK.getBlock().defaultBlockState().setValue(ComputerBlock.POWERED, true);
        BlockState cableState = BlockRegistry.CABLE_BLOCK.getBlock().defaultBlockState();
        BlockState audioState = BlockRegistry.AUDIO_STATION_BLOCK.getBlock().defaultBlockState();

        p.placeAndShow(audioPos, audioState);
        p.idle(20)
                .textKeyframe("The Audio Station plays sounds you uploaded to a Computer.", audioPos, 60)
                .idle(60);

        p.placeAndShow(computerPos, computerOn);
        p.traceCables(cableState, cablePos)
                .connectStart(Direction.WEST)
                .connectStart(Direction.EAST)
                .showAll(Direction.DOWN);
        p.idle(20)
                .textKeyframe("Wire it to a Computer on the same network.", audioPos, 50)
                .idle(60)
                .textKeyframe("Triggers can start and stop playback automatically.", computerPos, 60)
                .idle(60);

        scene.markAsFinished();
    }
}

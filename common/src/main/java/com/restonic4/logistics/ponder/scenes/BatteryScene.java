package com.restonic4.logistics.ponder.scenes;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.cable.CableBlock;
import com.restonic4.logistics.blocks.lamp.LampBlock;
import com.restonic4.logistics.ponder.util.CableRoute;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class BatteryScene {
    public static void battery(SceneBuilder scene, SceneBuildingUtil util) {
        PonderHelper p = PonderHelper.of(scene, util);
        p.init("battery", 5);

        BlockPos generatorPos = new BlockPos(1, 1, 2);
        BlockPos batteryPos = new BlockPos(2, 1, 2);
        BlockPos lampPos = new BlockPos(3, 1, 2);

        // The Battery only connects on its top and bottom, so power is routed over the top with cables.
        BlockPos cableOverGenerator = new BlockPos(1, 2, 2);
        BlockPos cableOverLamp = new BlockPos(3, 2, 2);

        BlockState generatorState = BlockRegistry.CREATIVE_GENERATOR_BLOCK.getBlock().defaultBlockState();
        BlockState batteryState = BlockRegistry.BATTERY_BLOCK.getBlock().defaultBlockState();
        BlockState lampOff = BlockRegistry.LAMP_BLOCK.getBlock().defaultBlockState().setValue(LampBlock.LIT, false);
        BlockState lampOn = lampOff.setValue(LampBlock.LIT, true);
        BlockState cableState = BlockRegistry.CABLE_BLOCK.getBlock().defaultBlockState();

        p.placeAndShow(batteryPos, batteryState);
        p.idle(20)
                .textKeyframe("The Battery stores energy, but only connects on its top and bottom.", batteryPos, 60)
                .idle(60);

        p.placeAndShow(generatorPos, generatorState).placeAndShow(lampPos, lampOn);

        // Cables along the top join generator, battery and lamp; each drops a connection straight down.
        CableRoute topLine = p.traceCables(cableState, cableOverGenerator, cableOverLamp);
        for (BlockPos pos : topLine.getAllPositions()) {
            p.getScene().world().modifyBlock(pos, s -> s.setValue(CableBlock.DOWN, true), false);
        }
        topLine.showAll(Direction.DOWN);

        p.idle(20)
                .textKeyframe("Route cables over the top to feed it while the network has spare energy.", batteryPos, 60)
                .idle(60);

        p.hide(cableOverGenerator, Direction.UP);
        p.hide(generatorPos, Direction.UP);
        p.idle(20)
                .textKeyframe("When production stops, the stored energy keeps your machines running.", lampPos, 60)
                .idle(60);

        scene.markAsFinished();
    }
}

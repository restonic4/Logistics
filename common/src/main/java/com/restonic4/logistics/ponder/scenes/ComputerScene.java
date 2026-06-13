package com.restonic4.logistics.ponder.scenes;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.computer.ComputerBlock;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class ComputerScene {
    public static void computer(SceneBuilder scene, SceneBuildingUtil util) {
        PonderHelper p = PonderHelper.of(scene, util);
        p.init("computer", 5);

        BlockPos generatorPos = new BlockPos(1, 1, 2);
        BlockPos cablePos = new BlockPos(2, 1, 2);
        BlockPos computerPos = new BlockPos(3, 1, 2);

        BlockState generatorState = BlockRegistry.CREATIVE_GENERATOR_BLOCK.getBlock().defaultBlockState();
        BlockState cableState = BlockRegistry.CABLE_BLOCK.getBlock().defaultBlockState();
        BlockState computerOff = BlockRegistry.COMPUTER_BLOCK.getBlock().defaultBlockState().setValue(ComputerBlock.POWERED, false);
        BlockState computerOn = computerOff.setValue(ComputerBlock.POWERED, true);

        p.placeAndShow(computerPos, computerOff);
        p.idle(20)
                .textKeyframe("The Computer is the brain of your network.", computerPos, 50)
                .idle(60);

        p.placeAndShow(generatorPos, generatorState);
        p.traceCables(cableState, cablePos)
                .connectStart(Direction.WEST)
                .connectStart(Direction.EAST)
                .showAll(Direction.DOWN);
        p.idle(20)
                .textKeyframe("Give it power and it boots up.", computerPos, 50)
                .idle(30);
        p.setBlock(computerPos, computerOn).idle(20);

        p.showInput(computerPos, 60).rightClick().build()
                .textKeyframe("Right-click to open its terminal.", computerPos, 50)
                .idle(60)
                .textKeyframe("Configure triggers and actions to automate the whole network.", computerPos, 60)
                .idle(60);

        scene.markAsFinished();
    }
}

package com.restonic4.logistics.ponder.scenes;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.computer.ComputerBlock;
import com.restonic4.logistics.blocks.redstone_reader.RedstoneReaderBlock;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneReaderScene {
    public static void redstoneReader(SceneBuilder scene, SceneBuildingUtil util) {
        PonderHelper p = PonderHelper.of(scene, util);
        p.init("redstone_reader", 5);

        BlockPos leverPos = new BlockPos(1, 1, 2);
        BlockPos readerPos = new BlockPos(2, 1, 2);
        BlockPos cablePos = new BlockPos(3, 1, 2);
        BlockPos computerPos = new BlockPos(4, 1, 2);

        BlockState redstoneSource = Blocks.REDSTONE_BLOCK.defaultBlockState();
        BlockState readerState = BlockRegistry.REDSTONE_READER_BLOCK.getBlock().defaultBlockState()
                .setValue(RedstoneReaderBlock.FACING, Direction.WEST);
        BlockState cableState = BlockRegistry.CABLE_BLOCK.getBlock().defaultBlockState();
        BlockState computerOn = BlockRegistry.COMPUTER_BLOCK.getBlock().defaultBlockState().setValue(ComputerBlock.POWERED, true);

        p.placeAndShow(readerPos, readerState);
        p.idle(20)
                .textKeyframe("The Redstone Reader senses redstone on its front face.", readerPos, 60)
                .idle(40);

        p.placeAndShow(leverPos, redstoneSource);
        p.idle(20)
                .textKeyframe("Its other five faces are normal cable faces.", readerPos, 50)
                .idle(40);

        p.placeAndShow(computerPos, computerOn);
        p.traceCables(cableState, cablePos)
                .connectStart(Direction.WEST)
                .connectStart(Direction.EAST)
                .showAll(Direction.DOWN);
        p.idle(20)
                .textKeyframe("A Computer can trigger on its signal: a single pulse, on, off, or a strength.", computerPos, 70)
                .idle(70);

        scene.markAsFinished();
    }
}

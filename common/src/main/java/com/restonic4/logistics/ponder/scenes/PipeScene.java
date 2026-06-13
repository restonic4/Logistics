package com.restonic4.logistics.ponder.scenes;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.accersor.AccessorBlock;
import com.restonic4.logistics.blocks.pipe.PipeBlock;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PipeScene {
    public static void pipe(SceneBuilder scene, SceneBuildingUtil util) {
        PonderHelper p = PonderHelper.of(scene, util);
        p.init("pipe", 5);

        BlockPos chestA = new BlockPos(0, 1, 2);
        BlockPos accessorA = new BlockPos(1, 1, 2);
        BlockPos pipePos = new BlockPos(2, 1, 2);
        BlockPos accessorB = new BlockPos(3, 1, 2);
        BlockPos chestB = new BlockPos(4, 1, 2);

        BlockState chestState = Blocks.CHEST.defaultBlockState();
        BlockState accessorWest = BlockRegistry.ACCESSOR_BLOCK.getBlock().defaultBlockState().setValue(AccessorBlock.FACING, Direction.WEST);
        BlockState accessorEast = BlockRegistry.ACCESSOR_BLOCK.getBlock().defaultBlockState().setValue(AccessorBlock.FACING, Direction.EAST);
        BlockState pipeState = BlockRegistry.PIPE_BLOCK.getBlock().defaultBlockState()
                .setValue(PipeBlock.WEST, true).setValue(PipeBlock.EAST, true);

        p.placeAndShow(chestA, chestState).placeAndShow(chestB, chestState);
        p.placeAndShow(accessorA, accessorWest).placeAndShow(accessorB, accessorEast);
        p.idle(20)
                .textKeyframe("Pipes move items between Accessors.", pipePos, 50)
                .idle(40);

        p.placeAndShow(pipePos, pipeState);
        p.idle(20)
                .textKeyframe("They form an item network, kept separate from energy.", pipePos, 60)
                .idle(60);

        scene.markAsFinished();
    }
}

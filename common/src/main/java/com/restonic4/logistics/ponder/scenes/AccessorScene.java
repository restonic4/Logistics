package com.restonic4.logistics.ponder.scenes;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.accersor.AccessorBlock;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class AccessorScene {
    public static void accessor(SceneBuilder scene, SceneBuildingUtil util) {
        PonderHelper p = PonderHelper.of(scene, util);
        p.init("accessor", 5);

        BlockPos chestPos = new BlockPos(1, 1, 2);
        BlockPos accessorPos = new BlockPos(2, 1, 2);

        BlockState chestState = Blocks.CHEST.defaultBlockState();
        BlockState accessorState = BlockRegistry.ACCESSOR_BLOCK.getBlock().defaultBlockState()
                .setValue(AccessorBlock.FACING, Direction.WEST);

        p.placeAndShow(chestPos, chestState);
        p.idle(20)
                .textKeyframe("The Accessor reads and writes an adjacent inventory.", accessorPos, 60)
                .idle(40);

        p.placeAndShow(accessorPos, accessorState);
        p.idle(20)
                .textKeyframe("Point its front face at a chest or machine.", accessorPos, 50)
                .idle(60)
                .textKeyframe("Bridged to a Computer, it can push and pull items.", accessorPos, 60)
                .idle(60);

        scene.markAsFinished();
    }
}

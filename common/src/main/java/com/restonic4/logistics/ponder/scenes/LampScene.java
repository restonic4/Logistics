package com.restonic4.logistics.ponder.scenes;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.lamp.LampBlock;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class LampScene {
    public static void lamp(SceneBuilder scene, SceneBuildingUtil util) {
        PonderHelper p = PonderHelper.of(scene, util);
        p.init("lamp", 5);

        BlockPos generatorPos = new BlockPos(1, 1, 2);
        BlockPos cablePos = new BlockPos(2, 1, 2);
        BlockPos lampPos = new BlockPos(3, 1, 2);

        BlockState generatorState = BlockRegistry.CREATIVE_GENERATOR_BLOCK.getBlock().defaultBlockState();
        BlockState cableState = BlockRegistry.CABLE_BLOCK.getBlock().defaultBlockState();
        BlockState lampOff = BlockRegistry.LAMP_BLOCK.getBlock().defaultBlockState().setValue(LampBlock.LIT, false);
        BlockState lampOn = lampOff.setValue(LampBlock.LIT, true);

        p.placeAndShow(lampPos, lampOff);
        p.idle(20)
                .textKeyframe("The Lamp turns network energy into light.", lampPos, 50)
                .idle(60);

        p.placeAndShow(generatorPos, generatorState);
        p.traceCables(cableState, cablePos)
                .connectStart(Direction.WEST)
                .connectStart(Direction.EAST)
                .showAll(Direction.DOWN);
        p.idle(20)
                .textKeyframe("Connect it to a powered network and it lights up.", lampPos, 50)
                .idle(20);
        p.setBlock(lampPos, lampOn).idle(40);

        scene.markAsFinished();
    }
}

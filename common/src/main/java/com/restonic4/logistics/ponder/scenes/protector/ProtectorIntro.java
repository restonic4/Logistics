package com.restonic4.logistics.ponder.scenes.protector;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class ProtectorIntro {
    public static void animate(PonderHelper p) {
        BlockPos protectorPos = new BlockPos(2, 1, 2);
        BlockState protectorState = BlockRegistry.PROTECTOR_BLOCK.getBlock().defaultBlockState();

        p.showInput(protectorPos, 20).rightClick().build()
                .textKeyframe(protectorPos, 20) // 1: Place
                .idle(20)
                .placeAndShow(protectorPos, protectorState, Direction.DOWN)
                .idle(20)
                .textKeyframe(protectorPos, 40) // 2: Needs components
                .idle(40)
                .hide(protectorPos, Direction.UP)
                .keyframe()
                .idle(40);
    }
}

package com.restonic4.logistics.ponder.scenes.protector;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.computer.ComputerBlock;
import com.restonic4.logistics.ponder.util.CableRoute;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class ProtectorComputer {
    public static void animate(PonderHelper p) {
        BlockPos protectorPos = new BlockPos(3, 1, 2);
        BlockPos computerPos = new BlockPos(1, 1, 2);

        BlockState protectorState = BlockRegistry.PROTECTOR_BLOCK.getBlock().defaultBlockState();
        BlockState cableState = BlockRegistry.CABLE_BLOCK.getBlock().defaultBlockState();
        BlockState computerOffState = BlockRegistry.COMPUTER_BLOCK.getBlock().defaultBlockState()
                .setValue(ComputerBlock.POWERED, false);

        p.placeAndShow(protectorPos, protectorState).placeAndShow(computerPos, computerOffState);

        CableRoute cableLine = p.traceCables(cableState, new BlockPos(2, 1, 2))
                .connectStart(Direction.WEST)  // Connect to computer
                .connectStart(Direction.EAST)  // Connect to protector
                .showAll(Direction.DOWN);

        p.idle(40)
                .textKeyframe(computerPos, 40) // 3: Computer we can config
                .idle(50)
                .showInput(computerPos, 60).rightClick().build()
                .textKeyframe(computerPos, 60) // 4: Right click, UI
                .idle(70)
                .textKeyframe(computerPos, 60) // 5: Need energy
                .idle(70);
    }
}

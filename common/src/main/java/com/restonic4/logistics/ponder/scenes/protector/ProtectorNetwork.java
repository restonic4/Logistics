package com.restonic4.logistics.ponder.scenes.protector;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.computer.ComputerBlock;
import com.restonic4.logistics.experiment.Items;
import com.restonic4.logistics.ponder.util.CableRoute;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ProtectorNetwork {
    public static void animate(PonderHelper p) {
        BlockPos protectorPos = new BlockPos(3, 1, 2);
        BlockPos computerPos = new BlockPos(1, 1, 2);

        BlockState cableState = BlockRegistry.CABLE_BLOCK.getBlock().defaultBlockState();
        BlockState computerOffState = BlockRegistry.COMPUTER_BLOCK.getBlock().defaultBlockState()
                .setValue(ComputerBlock.POWERED, false);
        BlockState computerOnState = computerOffState.setValue(ComputerBlock.POWERED, true);

        p.setBlock(computerPos, computerOnState);

        CableRoute cableLine = p.traceCables(cableState,
                new BlockPos(2, 1, 2),  // Intersection point
                new BlockPos(2, 1, 4)   // End point
        ).showFrom(1, Direction.DOWN);

        p.idle(40)
                .textKeyframe(cableLine.getEnd(), 40) // 6: Connect to network
                .idle(50)
                .textKeyframe(protectorPos, 80) // 7: Can break
                .idle(90)
                .textKeyframe(protectorPos, 80) // 8: Repair
                .showInput(protectorPos, 80).rightClick().withItem(new ItemStack(Items.KINETIC_CRYSTAL_SHARD.getItem())).build();
    }
}

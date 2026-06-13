package com.restonic4.logistics.ponder.scenes;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.computer.ComputerBlock;
import com.restonic4.logistics.blocks.lamp.LampBlock;
import com.restonic4.logistics.blocks.network_switch.NetworkSwitchBlock;
import com.restonic4.logistics.blocks.redstone_reader.RedstoneReaderBlock;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The headline example: a redstone light switch built entirely from network blocks. A lever on a
 * Redstone Reader drives Computer triggers that enable/disable a Network Switch face, which
 * connects or cuts the Lamp from the powered network.
 */
public class LightSwitchScene {
    public static void lightSwitch(SceneBuilder scene, SceneBuildingUtil util) {
        PonderHelper p = PonderHelper.of(scene, util);
        p.init("light_switch", 5);

        BlockPos generatorPos = new BlockPos(1, 1, 1);
        BlockPos computerPos = new BlockPos(2, 1, 1);
        BlockPos switchPos = new BlockPos(3, 1, 1);
        BlockPos readerPos = new BlockPos(1, 1, 2);
        BlockPos leverPos = new BlockPos(1, 1, 3);
        BlockPos lampPos = new BlockPos(3, 1, 2);

        BlockState generatorState = BlockRegistry.CREATIVE_GENERATOR_BLOCK.getBlock().defaultBlockState();
        BlockState computerOn = BlockRegistry.COMPUTER_BLOCK.getBlock().defaultBlockState().setValue(ComputerBlock.POWERED, true);
        // Switch starts with its face toward the lamp cut, so the lamp begins off.
        BlockState switchCut = BlockRegistry.NETWORK_SWITCH_BLOCK.getBlock().defaultBlockState().setValue(NetworkSwitchBlock.SOUTH, false);
        BlockState switchJoined = BlockRegistry.NETWORK_SWITCH_BLOCK.getBlock().defaultBlockState().setValue(NetworkSwitchBlock.SOUTH, true);
        BlockState readerState = BlockRegistry.REDSTONE_READER_BLOCK.getBlock().defaultBlockState().setValue(RedstoneReaderBlock.FACING, Direction.SOUTH);
        BlockState lampOff = BlockRegistry.LAMP_BLOCK.getBlock().defaultBlockState().setValue(LampBlock.LIT, false);
        BlockState lampOn = lampOff.setValue(LampBlock.LIT, true);
        BlockState leverOff = Blocks.LEVER.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.POWERED, false);
        BlockState leverOn = leverOff.setValue(BlockStateProperties.POWERED, true);

        p.placeAndShow(generatorPos, generatorState)
                .placeAndShow(computerPos, computerOn)
                .placeAndShow(switchPos, switchCut)
                .placeAndShow(lampPos, lampOff)
                .placeAndShow(readerPos, readerState);
        p.placeAndShow(leverPos, leverOff, Direction.NORTH);

        p.idle(20)
                .textKeyframe("A classic redstone light switch, built from the network.", lampPos, 60)
                .idle(60)
                .textKeyframe("The Computer reads a Redstone Reader and toggles a Network Switch.", computerPos, 70)
                .idle(70)
                .textKeyframe("The Switch sits between the power and the Lamp; the Reader watches the lever.", switchPos, 70)
                .idle(70);

        p.showInput(leverPos, 40).rightClick().build()
                .textKeyframe("Flip the lever on...", leverPos, 40)
                .idle(20);
        p.setBlock(leverPos, leverOn);
        p.idle(10)
                .textKeyframe("...its signal fires a trigger that enables the Switch face. Lights on!", lampPos, 70)
                .idle(20);
        p.setBlock(switchPos, switchJoined).setBlock(lampPos, lampOn);
        p.idle(60);

        p.showInput(leverPos, 40).rightClick().build();
        p.setBlock(leverPos, leverOff).setBlock(switchPos, switchCut).setBlock(lampPos, lampOff);
        p.idle(10)
                .textKeyframe("Flip it off and a second trigger cuts the face, splitting the network. Lights off.", lampPos, 80)
                .idle(80);

        scene.markAsFinished();
    }
}

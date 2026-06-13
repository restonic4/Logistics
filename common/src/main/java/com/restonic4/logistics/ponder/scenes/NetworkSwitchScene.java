package com.restonic4.logistics.ponder.scenes;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.lamp.LampBlock;
import com.restonic4.logistics.blocks.network_switch.NetworkSwitchBlock;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkSwitchScene {
    public static void networkSwitch(SceneBuilder scene, SceneBuildingUtil util) {
        PonderHelper p = PonderHelper.of(scene, util);
        p.init("network_switch", 5);

        BlockPos generatorPos = new BlockPos(1, 1, 2);
        BlockPos switchPos = new BlockPos(2, 1, 2);
        BlockPos lampPos = new BlockPos(3, 1, 2);

        BlockState generatorState = BlockRegistry.CREATIVE_GENERATOR_BLOCK.getBlock().defaultBlockState();
        BlockState switchOn = BlockRegistry.NETWORK_SWITCH_BLOCK.getBlock().defaultBlockState();
        BlockState switchCut = switchOn.setValue(NetworkSwitchBlock.EAST, false);
        BlockState lampOn = BlockRegistry.LAMP_BLOCK.getBlock().defaultBlockState().setValue(LampBlock.LIT, true);
        BlockState lampOff = lampOn.setValue(LampBlock.LIT, false);

        p.placeAndShow(generatorPos, generatorState)
                .placeAndShow(switchPos, switchOn)
                .placeAndShow(lampPos, lampOn);
        p.idle(20)
                .textKeyframe("Every face of the Network Switch is a cable face you can toggle.", switchPos, 60)
                .idle(60);

        p.textKeyframe("A Computer can disable a face...", switchPos, 50)
                .idle(50);
        p.setBlock(switchPos, switchCut).setBlock(lampPos, lampOff);
        p.idle(10)
                .textKeyframe("...splitting the network there, so the Lamp loses power.", lampPos, 60)
                .idle(60);

        p.setBlock(switchPos, switchOn).setBlock(lampPos, lampOn);
        p.idle(10)
                .textKeyframe("Re-enable it to reconnect. Pair it with a Redstone Reader for a light switch!", switchPos, 70)
                .idle(70);

        scene.markAsFinished();
    }
}

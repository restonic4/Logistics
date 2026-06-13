package com.restonic4.logistics.ponder.scenes;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.network_connector.NetworkConnectorBlock;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkConnectorScene {
    public static void networkConnector(SceneBuilder scene, SceneBuildingUtil util) {
        PonderHelper p = PonderHelper.of(scene, util);
        p.init("network_connector", 5);

        BlockPos cablePos = new BlockPos(1, 1, 2);
        BlockPos connectorPos = new BlockPos(2, 1, 2);
        BlockPos pipePos = new BlockPos(3, 1, 2);

        BlockState cableState = BlockRegistry.CABLE_BLOCK.getBlock().defaultBlockState()
                .setValue(com.restonic4.logistics.blocks.cable.CableBlock.EAST, true);
        BlockState connectorState = BlockRegistry.NETWORK_CONNECTOR_BLOCK.getBlock().defaultBlockState()
                .setValue(NetworkConnectorBlock.FACING, Direction.EAST);
        BlockState pipeState = BlockRegistry.PIPE_BLOCK.getBlock().defaultBlockState()
                .setValue(com.restonic4.logistics.blocks.pipe.PipeBlock.WEST, true);

        p.placeAndShow(connectorPos, connectorState);
        p.idle(20)
                .textKeyframe("The Network Connector bridges two separate networks.", connectorPos, 60)
                .idle(60);

        p.placeAndShow(cablePos, cableState).placeAndShow(pipePos, pipeState);
        p.idle(20)
                .textKeyframe("Energy on one side, items on the other.", connectorPos, 50)
                .idle(60)
                .textKeyframe("This is how a Computer reaches your item network.", connectorPos, 60)
                .idle(60);

        scene.markAsFinished();
    }
}

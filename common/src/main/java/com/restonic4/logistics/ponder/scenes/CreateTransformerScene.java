package com.restonic4.logistics.ponder.scenes;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.lamp.LampBlock;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Resolves the Create Transformer (and a Create shaft for clarity) through the registry instead of
 * importing them, so this scene lives in common without depending on the Create compatibility
 * module. It is only ever reachable when Create is present.
 */
public class CreateTransformerScene {
    public static void createTransformer(SceneBuilder scene, SceneBuildingUtil util) {
        PonderHelper p = PonderHelper.of(scene, util);
        p.init("create_transformer", 5);

        // Transformer faces WEST: it takes rotation on the shaft side (its back, EAST) and outputs
        // energy on the front (WEST) face, where the cable connects.
        BlockPos lampPos = new BlockPos(1, 1, 2);
        BlockPos cablePos = new BlockPos(2, 1, 2);
        BlockPos transformerPos = new BlockPos(3, 1, 2);
        BlockPos shaftPos = new BlockPos(4, 1, 2);

        BlockState lampOn = BlockRegistry.LAMP_BLOCK.getBlock().defaultBlockState().setValue(LampBlock.LIT, true);
        BlockState cableState = BlockRegistry.CABLE_BLOCK.getBlock().defaultBlockState();
        BlockState transformerState = BuiltInRegistries.BLOCK.get(Logistics.id("create_transformer")).defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.WEST);
        BlockState shaftState = BuiltInRegistries.BLOCK.get(new ResourceLocation("create", "shaft")).defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X);

        p.placeAndShow(transformerPos, transformerState);
        p.placeAndShow(shaftPos, shaftState, Direction.WEST);
        p.idle(20)
                .textKeyframe("The Create Transformer takes rotation on its shaft side (the back).", shaftPos, 60)
                .idle(60);

        p.placeAndShow(lampPos, lampOn);
        p.traceCables(cableState, cablePos)
                .connectStart(Direction.WEST)
                .connectStart(Direction.EAST)
                .showAll(Direction.DOWN);
        p.idle(20)
                .textKeyframe("It outputs energy from the opposite face into the network.", transformerPos, 70)
                .idle(70);

        scene.markAsFinished();
    }
}

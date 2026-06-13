package com.restonic4.logistics.ponder.scenes;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.BlockRegistry;
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
 * Resolves the Create Motor (and a Create shaft for clarity) through the registry instead of
 * importing them, so this scene lives in common without depending on the Create compatibility
 * module. It is only ever reachable when Create is present.
 */
public class CreateMotorScene {
    public static void createMotor(SceneBuilder scene, SceneBuildingUtil util) {
        PonderHelper p = PonderHelper.of(scene, util);
        p.init("create_motor", 5);

        // Motor faces EAST: its output shaft points east, and it only accepts energy on the opposite
        // (back/WEST) face. So the cable must come from behind.
        BlockPos generatorPos = new BlockPos(1, 1, 2);
        BlockPos cablePos = new BlockPos(2, 1, 2);
        BlockPos motorPos = new BlockPos(3, 1, 2);
        BlockPos shaftPos = new BlockPos(4, 1, 2);

        BlockState generatorState = BlockRegistry.CREATIVE_GENERATOR_BLOCK.getBlock().defaultBlockState();
        BlockState cableState = BlockRegistry.CABLE_BLOCK.getBlock().defaultBlockState();
        BlockState motorState = BuiltInRegistries.BLOCK.get(Logistics.id("create_motor")).defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.EAST);
        BlockState shaftState = BuiltInRegistries.BLOCK.get(new ResourceLocation("create", "shaft")).defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X);

        p.placeAndShow(motorPos, motorState);
        p.placeAndShow(shaftPos, shaftState, Direction.WEST);
        p.idle(20)
                .textKeyframe("The Create Motor drives a rotating shaft from its front face.", shaftPos, 60)
                .idle(60);

        p.placeAndShow(generatorPos, generatorState);
        p.traceCables(cableState, cablePos)
                .connectStart(Direction.WEST)
                .connectStart(Direction.EAST)
                .showAll(Direction.DOWN);
        p.idle(20)
                .textKeyframe("Feed it energy from behind, opposite the shaft, to drive your contraptions.", motorPos, 70)
                .idle(70);

        scene.markAsFinished();
    }
}

package com.restonic4.logistics.compatibility.create.blocks.motor;

import com.restonic4.logistics.blocks.base.NetworkBlock;
import com.restonic4.logistics.compatibility.create.CreateCompatibility;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class CreateMotorBlock extends DirectionalKineticBlock implements NetworkBlock, IBE<CreateMotorBlockEntity> {
    public CreateMotorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return AllShapes.MOTOR_BLOCK.get((Direction)state.getValue(FACING));
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = this.getPreferredFacing(context);
        return (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) && preferred != null ? (BlockState)this.defaultBlockState().setValue(FACING, preferred) : super.getStateForPlacement(context);
    }

    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    public Direction.Axis getRotationAxis(BlockState state) {
        return ((Direction)state.getValue(FACING)).getAxis();
    }

    public boolean isPathfindable(@NotNull BlockState state, @NotNull BlockGetter reader, @NotNull BlockPos pos, @NotNull PathComputationType type) {
        return false;
    }

    public Class<CreateMotorBlockEntity> getBlockEntityClass() {
        return CreateMotorBlockEntity.class;
    }

    public BlockEntityType<? extends CreateMotorBlockEntity> getBlockEntityType() {
        return CreateCompatibility.CREATE_MOTOR.getBlockEntityType(CreateMotorBlockEntity.class);
    }

    @Override
    public NodeTypeRegistry.NetworkNodeType<?> getNodeType() {
        return CreateCompatibility.CREATE_MOTOR.getNodeType();
    }

    @Override
    public void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState oldBlockState, boolean isMoving) {
        super.onPlace(blockState, level, blockPos, oldBlockState, isMoving);

        if (blockState.is(oldBlockState.getBlock())) return;

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            BlockPos immutableBlockPos = blockPos.immutable(); // This is necessary because otherwise it brutally implodes with /fill commands

            NetworkNode newNode = getNodeType().create(immutableBlockPos);
            onNodeCreated(newNode, serverLevel, immutableBlockPos);
            NetworkManager.get(serverLevel).onMemberPlaced(newNode);
        }
    }

    @Override
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState newBlockState, boolean isMoving) {
        if (!blockState.is(newBlockState.getBlock())) {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                BlockPos immutableBlockPos = blockPos.immutable();
                NetworkManager.get(serverLevel).onMemberRemoved(immutableBlockPos);
            }
        }

        super.onRemove(blockState, level, blockPos, newBlockState, isMoving);
    }
}

package com.restonic4.logistics.blocks.charging_station;

import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.blocks.base.InvertiblePlacement;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

public class ChargingStationBlock extends BaseNetworkBlock implements EntityBlock, InvertiblePlacement {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ChargingStationBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return applyShiftInversion(context, state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.CONSUME;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        NetworkNode node = NetworkManager.get((ServerLevel) level).getNodeByBlockPos(pos);
        if (!(node instanceof ChargingStationNode chargingStationNode)) return InteractionResult.PASS;

        ItemStack stationItem = chargingStationNode.getHeldItem();
        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHandItem = player.getItemInHand(InteractionHand.OFF_HAND);

        InteractionHand targetHand = null;
        ItemStack playerStack = ItemStack.EMPTY;
        boolean isValidEnergyItem = false;

        if (!mainHandItem.isEmpty()) {
            targetHand = InteractionHand.MAIN_HAND;
            playerStack = mainHandItem;
            isValidEnergyItem = EnergyItemHelper.isEnergyItem(playerStack);
        } else if (!offHandItem.isEmpty()) {
            targetHand = InteractionHand.OFF_HAND;
            playerStack = offHandItem;
            isValidEnergyItem = EnergyItemHelper.isEnergyItem(playerStack);
        }

        // SCENARIO A: Both player hands are completely empty
        if (playerStack.isEmpty()) {
            if (stationItem.isEmpty()) {
                return InteractionResult.PASS; // Nothing to extract, do nothing
            }

            // Extract straight into the main hand
            chargingStationNode.setHeldItem(ItemStack.EMPTY);
            updateRenderer((ServerLevel) level, pos, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.MAIN_HAND, stationItem);

            player.swing(InteractionHand.MAIN_HAND, true);
            return InteractionResult.SUCCESS;
        }

        // SCENARIO B: Player is holding a VALID energy item
        if (isValidEnergyItem) {
            // Swap whatever is in the station with the player's held energy item
            chargingStationNode.setHeldItem(playerStack);
            updateRenderer((ServerLevel) level, pos, playerStack);
            player.setItemInHand(targetHand, stationItem);

            player.swing(targetHand, true);
            return InteractionResult.SUCCESS;
        }

        // SCENARIO C: Player is holding an INVALID item
        // If the station has something, extract it to inventory (or drop it if full).
        // If the station is empty, completely ignore the action.
        if (!stationItem.isEmpty()) {
            chargingStationNode.setHeldItem(ItemStack.EMPTY);
            updateRenderer((ServerLevel) level, pos, ItemStack.EMPTY);

            // Try adding to inventory. If full, remaining items left in 'stationItem' are popped into the world
            if (!player.getInventory().add(stationItem)) {
                Block.popResource(level, pos, stationItem);
            }

            player.swing(targetHand, true);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public void updateRenderer(ServerLevel serverLevel, BlockPos pos, ItemStack newVisualStack) {
        BlockEntity be = serverLevel.getBlockEntity(pos);
        if (be instanceof ChargingStationBlockEntity stationBe) {
            stationBe.setRenderStack(newVisualStack);
        }
    }

    @Override
    public void playerDestroy(Level level, @NotNull Player player, @NotNull BlockPos pos, @NotNull BlockState state, BlockEntity blockEntity, @NotNull ItemStack tool) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            NetworkNode node = NetworkManager.get(serverLevel).getNodeByBlockPos(pos);
            if (node instanceof ChargingStationNode station) {
                ItemStack held = station.getHeldItem();
                if (!held.isEmpty()) {
                    Block.popResource(level, pos, held);
                }
            }
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    public Set<Direction> getAllowedConnections(BlockState state) {
        return EnumSet.of(Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargingStationBlockEntity(pos, state);
    }

    @Override
    public Property<Direction> getFacingProperty() {
        return FACING;
    }
}
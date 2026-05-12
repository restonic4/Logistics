package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.blocks.network_connector.NetworkConnectorNode;
import com.restonic4.logistics.experiment.Sounds;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.networks.types.ItemNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComputerBlock extends BaseNetworkBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public ComputerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkNode node = NetworkManager.get((ServerLevel) level).getNodeByBlockPos(pos);
            if (node instanceof ComputerNode computerNode && computerNode.isPowered() && node.getNetwork() instanceof EnergyNetwork energyNetwork) {
                Set<ItemNetwork> itemNetworks = new HashSet<>();
                for (NetworkConnectorNode connectorNode : energyNetwork.getNetworkConnectors()) {
                    if (connectorNode.getFacingNetwork() instanceof ItemNetwork itemNetwork) {
                        itemNetworks.add(itemNetwork);
                    }
                }

                List<ComputerSyncPacket.AccessorData> accessors = new ArrayList<>();

                for (ItemNetwork itemNetwork : itemNetworks) {
                    for (NetworkNode networkNode : itemNetwork.getNodeIndex().getAllNodes()) {
                        if (networkNode instanceof AccessorNode accessorNode) {
                            accessors.add(new ComputerSyncPacket.AccessorData(accessorNode.getBlockPos(), accessorNode.getVirtualInventory((ServerLevel) level)));
                        }
                    }
                }

                ServerNetworking.sendToClient(serverPlayer, new ComputerSyncPacket(node.getBlockPos(), accessors));
                level.playSound(null, pos, Sounds.COMPUTER_BOOT.getSoundEvent(), SoundSource.BLOCKS, 1.0F, 1.0F);
                level.playSound(null, pos, Sounds.COMPUTER_BOOT_BEEP.getSoundEvent(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }

        return InteractionResult.CONSUME;
    }

    private static final VoxelShape SHAPE_NORTH = Block.box(1.0, 1.0, 0.0,  15.0, 15.0, 14.0); // back=2 (Z+), front=14
    private static final VoxelShape SHAPE_SOUTH = Block.box(1.0, 1.0, 2.0,  15.0, 15.0, 16.0); // back=2 (Z-), front=2
    private static final VoxelShape SHAPE_EAST  = Block.box(2.0, 1.0, 1.0,  16.0, 15.0, 15.0); // back=2 (X-), front=2
    private static final VoxelShape SHAPE_WEST  = Block.box(0.0, 1.0, 1.0,  14.0, 15.0, 15.0); // back=2 (X+), front=14

    private static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST  -> SHAPE_EAST;
            case WEST  -> SHAPE_WEST;
            default    -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }
}
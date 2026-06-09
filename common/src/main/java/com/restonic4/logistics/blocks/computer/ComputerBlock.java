package com.restonic4.logistics.blocks.computer;

import com.mojang.authlib.GameProfile;
import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.blocks.base.InvertiblePlacement;
import com.restonic4.logistics.blocks.computer.protection.ProtectionEditSyncPacket;
import com.restonic4.logistics.blocks.protector.ProtectorNode;
import com.restonic4.logistics.blocks.protector.data_types.ProtectionZone;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static com.restonic4.logistics.utils.MinecraftUtils.getRelativeDown;
import static com.restonic4.logistics.utils.MinecraftUtils.getRelativeRight;

public class ComputerBlock extends BaseNetworkBlock implements InvertiblePlacement {
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = (ServerLevel) level;
            NetworkNode node = NetworkManager.get(serverLevel).getNodeByBlockPos(pos);
            if (node instanceof ComputerNode computerNode && computerNode.isPowered() && node.getNetwork() instanceof EnergyNetwork energyNetwork) {
                List<ProtectionZone> zones = new ArrayList<>();
                for (ProtectorNode protectorNode : energyNetwork.getProtectors()) {
                    zones.add(new ProtectionZone(
                            protectorNode.getUUID(),
                            protectorNode.getBlockPos(),
                            protectorNode.getRadius(),
                            protectorNode.isCreative(),
                            protectorNode.getRoles(),
                            protectorNode.isPowered()
                    ));
                }

                ServerNetworking.sendToClient(serverPlayer, new ComputerSyncPacket(node.getBlockPos(), !zones.isEmpty()));
                List<ComputerLogEntry> logEntries = ComputerLogger.get(serverLevel).getEntries(pos);
                ServerNetworking.sendToClient(serverPlayer, new ComputerLogSyncPacket(pos, logEntries));
                level.playSound(null, pos, Sounds.COMPUTER_OPEN.getSoundEvent(), SoundSource.BLOCKS, 1.0F, 1.0F);

                List<GameProfile> profiles = serverLevel.getServer().getPlayerList().getPlayers().stream().map(ServerPlayer::getGameProfile).collect(Collectors.toList());
                ServerNetworking.sendToClient(serverPlayer, new ProtectionEditSyncPacket(pos, zones, profiles));
            }
        }

        return InteractionResult.CONSUME;
    }

    private static final VoxelShape SHAPE_NORTH = Block.box(1.0, 1.0, 0.0,  15.0, 15.0, 14.0);
    private static final VoxelShape SHAPE_SOUTH = Block.box(1.0, 1.0, 2.0,  15.0, 15.0, 16.0);
    private static final VoxelShape SHAPE_EAST  = Block.box(2.0, 1.0, 1.0,  16.0, 15.0, 15.0);
    private static final VoxelShape SHAPE_WEST  = Block.box(0.0, 1.0, 1.0,  14.0, 15.0, 15.0);

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

    @Override
    public Set<Direction> getAllowedConnections(BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction right = getRelativeRight(facing);

        return EnumSet.of(
                facing.getOpposite(),
                getRelativeDown(facing),
                right,
                right.getOpposite()
        );
    }

    @Override
    public Property<Direction> getFacingProperty() {
        return FACING;
    }
}
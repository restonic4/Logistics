package com.restonic4.logistics.blocks.network_switch;

import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * A network node whose six faces are all cable faces, each independently enable/disable-able by the
 * computer. The per-face state lives in the blockstate (the booleans below) because network
 * connectivity is decided purely from blockstate ({@link com.restonic4.logistics.blocks.base.NetworkBlock#canConnectOnSide}).
 * Disabling a face makes {@link #getAllowedConnections} drop it, so the standard merge/split logic
 * cuts the network there — and re-enabling merges it back. Faces default to enabled (acts like a
 * plain cable until the computer cuts it).
 */
public class NetworkSwitchBlock extends BaseNetworkBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST,
            Direction.UP, UP,
            Direction.DOWN, DOWN
    );

    public NetworkSwitchBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, true).setValue(EAST, true)
                .setValue(SOUTH, true).setValue(WEST, true)
                .setValue(UP, true).setValue(DOWN, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public Set<Direction> getAllowedConnections(BlockState state) {
        Set<Direction> connections = EnumSet.noneOf(Direction.class);
        for (Map.Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
            if (state.getValue(entry.getValue())) connections.add(entry.getKey());
        }
        return connections;
    }

    /** Whether the switch at {@code pos} currently conducts on {@code dir}. False if it is not a switch. */
    public static boolean isFaceEnabled(ServerLevel level, net.minecraft.core.BlockPos pos, Direction dir) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof NetworkSwitchBlock)) return false;
        return state.getValue(PROPERTY_BY_DIRECTION.get(dir));
    }

    /**
     * Enables or disables one face and recomputes the network topology around it. No-op if the face
     * is already in the requested state or the block is not a switch.
     */
    public static void setFaceEnabled(ServerLevel level, net.minecraft.core.BlockPos pos, Direction dir, boolean enabled) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof NetworkSwitchBlock)) return;

        BooleanProperty property = PROPERTY_BY_DIRECTION.get(dir);
        if (state.getValue(property) == enabled) return;

        level.setBlock(pos, state.setValue(property, enabled), 3);

        NetworkManager manager = NetworkManager.get(level);
        NetworkNode node = manager.getNodeByBlockPos(pos);
        if (node != null) manager.refreshMembership(node);
    }
}

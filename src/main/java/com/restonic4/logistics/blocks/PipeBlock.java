package com.restonic4.logistics.blocks;

import com.restonic4.logistics.energy.EnergyConnectable;
import com.restonic4.logistics.energy.EnergyNetworkManager;
import com.restonic4.logistics.energy.EnergyNodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class PipeBlock extends Block implements EnergyConnectable {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
            Direction.NORTH, NORTH,
            Direction.EAST,  EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST,  WEST,
            Direction.UP,    UP,
            Direction.DOWN,  DOWN
    );

    public PipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    // -------------------------------------------------------------------------
    // Placement / removal — hook the network manager here
    // -------------------------------------------------------------------------

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.makeConnections(context.getLevel(), context.getClickedPos());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        // Only run on the server — the manager lives server-side only.
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            EnergyNetworkManager.get(serverLevel).onMemberPlaced(serverLevel, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Only fire if the block is actually changing (not just state update).
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                EnergyNetworkManager.get(serverLevel).onMemberRemoved(serverLevel, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // -------------------------------------------------------------------------
    // Connection logic (visual + network-aware)
    // -------------------------------------------------------------------------

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        boolean connects = this.canConnectTo(neighborState);
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connects);
    }

    public BlockState makeConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = this.defaultBlockState();
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), this.canConnectTo(neighborState));
        }
        return state;
    }

    /**
     * Controls which blocks this pipe visually connects to.
     *
     * - Other pipes: always connect (topology).
     * - EnergyConnectable blocks: connect (other pipe types you add later).
     * - Blocks whose BlockEntity implements IEnergyNode: connect (machines, batteries).
     *
     * AmethystBlock / TorchBlock were test placeholders — removed. Add your real
     * machine blocks here, or better: check for the IEnergyNode interface on their BE.
     */
    private boolean canConnectTo(BlockState state) {
        // Pipe-to-pipe
        if (state.getBlock() instanceof EnergyConnectable) return true;

        // Pipe-to-machine: the block's entity must opt in via IEnergyNode.
        // We can't call level.getBlockEntity() here (we only have BlockState),
        // so we check a marker interface on the Block class itself.
        // Your machine blocks should implement EnergyConnectable OR their BE
        // implements IEnergyNode — use EnergyNodeBlock marker for the latter.
        if (state.getBlock() instanceof EnergyNodeBlock) return true;

        return false;
    }

    // -------------------------------------------------------------------------
    // VoxelShape
    // -------------------------------------------------------------------------

    private static final VoxelShape CORE        = Block.box(6, 6,  6,  10, 10, 10);
    private static final VoxelShape UP_SHAPE    = Block.box(6, 10, 6,  10, 16, 10);
    private static final VoxelShape DOWN_SHAPE  = Block.box(6, 0,  6,  10, 6,  10);
    private static final VoxelShape NORTH_SHAPE = Block.box(6, 6,  0,  10, 10, 6);
    private static final VoxelShape EAST_SHAPE  = Block.box(10, 6, 6,  16, 10, 10);
    private static final VoxelShape SOUTH_SHAPE = Block.box(6, 6,  10, 10, 10, 16);
    private static final VoxelShape WEST_SHAPE  = Block.box(0, 6,  6,  6,  10, 10);

    private VoxelShape computeShape(BlockState state) {
        VoxelShape shape = CORE;
        if (state.getValue(UP))    shape = Shapes.or(shape, UP_SHAPE);
        if (state.getValue(DOWN))  shape = Shapes.or(shape, DOWN_SHAPE);
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_SHAPE);
        if (state.getValue(EAST))  shape = Shapes.or(shape, EAST_SHAPE);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (state.getValue(WEST))  shape = Shapes.or(shape, WEST_SHAPE);
        return shape;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return computeShape(state);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) { return true; }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return computeShape(state);
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return 1.0F; }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
}
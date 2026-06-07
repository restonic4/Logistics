package com.restonic4.logistics.ponder.util;

import com.restonic4.logistics.blocks.cable.CableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class CableRoute {
    private final List<BlockPos> path = new ArrayList<>();
    private final PonderHelper helper;

    private CableRoute(PonderHelper helper, BlockState baseState, BlockPos... corners) {
        this.helper = helper;
        generatePath(corners);
        placeCables(baseState);
    }

    public static CableRoute trace(PonderHelper helper, BlockState baseState, BlockPos... corners) {
        return new CableRoute(helper, baseState, corners);
    }

    private void generatePath(BlockPos[] corners) {
        for (int i = 0; i < corners.length - 1; i++) {
            BlockPos current = corners[i];
            BlockPos target = corners[i + 1];

            int dx = Integer.signum(target.getX() - current.getX());
            int dy = Integer.signum(target.getY() - current.getY());
            int dz = Integer.signum(target.getZ() - current.getZ());

            BlockPos step = current;
            while (!step.equals(target)) {
                if (path.isEmpty() || !path.get(path.size() - 1).equals(step)) {
                    path.add(step);
                }
                step = step.offset(dx, dy, dz);
            }
        }
        if (corners.length > 0) {
            BlockPos last = corners[corners.length - 1];
            if (path.isEmpty() || !path.get(path.size() - 1).equals(last)) {
                path.add(last);
            }
        }
    }

    private void placeCables(BlockState baseState) {
        for (int i = 0; i < path.size(); i++) {
            BlockPos pos = path.get(i);

            Direction dirToPrev = (i > 0) ? getDirection(pos, path.get(i - 1)) : null;
            Direction dirToNext = (i < path.size() - 1) ? getDirection(pos, path.get(i + 1)) : null;

            if (helper.hasCable(pos)) {
                // INTERSECTION: Safe to use modifyBlock because the block physically exists
                helper.getScene().world().modifyBlock(pos, currentState -> {
                    BlockState stateToModify = (currentState.getBlock() instanceof CableBlock) ? currentState : baseState;
                    if (dirToPrev != null) stateToModify = stateToModify.setValue(CableBlock.PROPERTY_BY_DIRECTION.get(dirToPrev), true);
                    if (dirToNext != null) stateToModify = stateToModify.setValue(CableBlock.PROPERTY_BY_DIRECTION.get(dirToNext), true);
                    return stateToModify;
                }, false);
            } else {
                // NEW CABLE: We MUST use setBlock so Ponder renders it!
                BlockState newState = baseState;
                if (dirToPrev != null) newState = newState.setValue(CableBlock.PROPERTY_BY_DIRECTION.get(dirToPrev), true);
                if (dirToNext != null) newState = newState.setValue(CableBlock.PROPERTY_BY_DIRECTION.get(dirToNext), true);
                helper.setBlock(pos, newState); // Uses our helper to automatically register to memory
            }
        }
    }

    private Direction getDirection(BlockPos from, BlockPos to) {
        BlockPos diff = to.subtract(from);
        for (Direction dir : Direction.values()) {
            if (dir.getNormal().getX() == diff.getX() &&
                    dir.getNormal().getY() == diff.getY() &&
                    dir.getNormal().getZ() == diff.getZ()) {
                return dir;
            }
        }
        return null;
    }

    public BlockPos getStart() { return path.isEmpty() ? BlockPos.ZERO : path.get(0); }
    public BlockPos getEnd() { return path.isEmpty() ? BlockPos.ZERO : path.get(path.size() - 1); }

    public CableRoute showAll(Direction slideDirection) {
        for (BlockPos pos : path) helper.getScene().world().showSection(helper.getUtil().select().position(pos), slideDirection);
        return this;
    }

    /** Easily show a partial route (perfect for skipping intersections) */
    public CableRoute showFrom(int startIndex, Direction slideDirection) {
        for (int i = startIndex; i < path.size(); i++) {
            helper.getScene().world().showSection(helper.getUtil().select().position(path.get(i)), slideDirection);
        }
        return this;
    }

    public CableRoute hideAll(Direction slideDirection) {
        for (BlockPos pos : path) helper.getScene().world().hideSection(helper.getUtil().select().position(pos), slideDirection);
        return this;
    }

    public CableRoute connectStart(Direction direction) {
        if (!path.isEmpty()) {
            helper.getScene().world().modifyBlock(getStart(), state -> state.setValue(CableBlock.PROPERTY_BY_DIRECTION.get(direction), true), false);
        }
        return this;
    }

    public CableRoute connectEnd(Direction direction) {
        if (!path.isEmpty()) {
            helper.getScene().world().modifyBlock(getEnd(), state -> state.setValue(CableBlock.PROPERTY_BY_DIRECTION.get(direction), true), false);
        }
        return this;
    }

    public List<BlockPos> getAllPositions() {
        return this.path;
    }
}
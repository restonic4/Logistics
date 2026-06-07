package com.restonic4.logistics.ponder.util;

import com.restonic4.logistics.blocks.cable.CableBlock;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class PonderHelper {
    private final SceneBuilder scene;
    private final SceneBuildingUtil util;

    // BUILD-TIME MEMORY: Tracks where we explicitly placed cables
    private final Set<BlockPos> placedCables = new HashSet<>();

    private PonderHelper(SceneBuilder scene, SceneBuildingUtil util) {
        this.scene = scene;
        this.util = util;
    }

    public static PonderHelper of(SceneBuilder scene, SceneBuildingUtil util) {
        return new PonderHelper(scene, util);
    }

    public SceneBuilder getScene() { return scene; }
    public SceneBuildingUtil getUtil() { return util; }

    public void addCable(BlockPos pos) { placedCables.add(pos); }
    public boolean hasCable(BlockPos pos) { return placedCables.contains(pos); }

    public PonderHelper init(String sceneID, int basePlateSize) {
        this.scene.title(sceneID, "header");
        this.scene.configureBasePlate(0, 0, basePlateSize);

        this.idle(10);
        this.scene.showBasePlate();
        this.idle(20);

        return this;
    }

    // --- Time & Flow ---

    public PonderHelper idle(int ticks) {
        scene.idle(ticks);
        return this;
    }

    public PonderHelper keyframe() {
        scene.addKeyframe();
        return this;
    }

    // --- Block Manipulation ---

    public PonderHelper setBlock(BlockPos pos, BlockState state) {
        scene.world().setBlock(pos, state, false);

        // Smart tracking
        if (state.getBlock() instanceof CableBlock) {
            placedCables.add(pos);
        } else {
            placedCables.remove(pos);
        }
        return this;
    }

    public PonderHelper show(BlockPos pos, Direction slideDirection) {
        scene.world().showSection(util.select().position(pos), slideDirection);
        return this;
    }

    public PonderHelper show(BlockPos pos) {
        return show(pos, Direction.DOWN);
    }

    public PonderHelper hide(BlockPos pos, Direction slideDirection) {
        scene.world().hideSection(util.select().position(pos), slideDirection);
        return this;
    }

    public PonderHelper hide(BlockPos pos) {
        return hide(pos, Direction.UP);
    }

    public PonderHelper placeAndShow(BlockPos pos, BlockState state, Direction slideDirection) {
        return setBlock(pos, state).show(pos, slideDirection);
    }

    public PonderHelper placeAndShow(BlockPos pos, BlockState state) {
        return placeAndShow(pos, state, Direction.DOWN);
    }

    // --- Overlay & Text ---

    public PonderHelper text(String text, BlockPos pointAt, Direction surfaceDir, int ticks) {
        scene.overlay().showText(ticks)
                .text(text)
                .pointAt(util.vector().blockSurface(pointAt, surfaceDir));
        return this;
    }

    public PonderHelper textKeyframe(String text, BlockPos pointAt, int ticks) {
        scene.overlay().showText(ticks)
                .text(text)
                .pointAt(util.vector().blockSurface(pointAt, Direction.UP))
                .attachKeyFrame();
        return this;
    }

    public PonderHelper textKeyframe(BlockPos pointAt, int ticks) {
        return textKeyframe("generated", pointAt, ticks);
    }

    public CableRoute traceCables(BlockState baseState, BlockPos... corners) {
        return CableRoute.trace(this, baseState, corners);
    }

    public InputBuilder showInput(BlockPos pos, int ticks) {
        return new InputBuilder(this, pos, ticks);
    }
}
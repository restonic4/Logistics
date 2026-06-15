package com.restonic4.logistics.ponder.scenes;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Joke scene: the full 34x34x34 backrooms structure, revealed one Y layer per second.
 * No annotations beyond the punchline, we just let the structure build itself up.
 */
public class BackroomsScene {
    private static final int SIZE = 34;

    // Nudge the camera off-centre so the huge structure frames a bit nicer.
    // Positive X moves the view right, positive Z moves it back. Flip the signs if it pans the wrong way.
    private static final double CAMERA_RIGHT = 14.0;
    private static final double CAMERA_BACK = 4.0;

    public static void backrooms(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("backrooms", "Backrooms");
        scene.configureBasePlate(0, 0, SIZE);
        scene.idle(10);

        // Shift the point of interest; nothing re-focuses it afterwards, so it sticks for the whole scene.
        scene.addInstruction(s -> s.setPointOfInterest(
                s.getPointOfInterest().add(new Vec3(CAMERA_RIGHT, 0, CAMERA_BACK))));

        // The punchline, then we say nothing else.
        scene.overlay().showText(80)
                .text("Just do it...")
                .pointAt(util.vector().topOf(SIZE / 2, 0, SIZE / 2))
                .attachKeyFrame();
        scene.idle(30);

        // Reveal the structure layer by layer, roughly one per second.
        for (int y = 0; y < SIZE; y++) {
            scene.world().showSection(util.select().layer(y), Direction.DOWN);
            scene.idle(20);
        }

        scene.markAsFinished();
    }
}

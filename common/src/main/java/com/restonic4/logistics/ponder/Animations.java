package com.restonic4.logistics.ponder;

import com.restonic4.logistics.ponder.scenes.protector.ProtectorComputer;
import com.restonic4.logistics.ponder.scenes.protector.ProtectorIntro;
import com.restonic4.logistics.ponder.scenes.protector.ProtectorNetwork;
import com.restonic4.logistics.ponder.util.PonderHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;

public class Animations {
    public static void protector(SceneBuilder scene, SceneBuildingUtil util) {
        PonderHelper p = PonderHelper.of(scene, util);
        p.init("protector", 5);

        ProtectorIntro.animate(p);
        ProtectorComputer.animate(p);
        ProtectorNetwork.animate(p);

        scene.markAsFinished();
    }
}
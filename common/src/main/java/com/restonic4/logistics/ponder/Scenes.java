package com.restonic4.logistics.ponder;

import com.restonic4.logistics.Logistics;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class Scenes {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(Logistics.id("protector"), Logistics.id("creative_protector"))
                .addStoryBoard("scenes/floor_5", Animations::protector);
    }
}

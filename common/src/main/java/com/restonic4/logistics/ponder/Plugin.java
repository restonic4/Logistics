package com.restonic4.logistics.ponder;

import com.restonic4.logistics.Constants;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class Plugin implements PonderPlugin {

    @Override
    public @NotNull String getModId() {
        return Constants.MOD_ID;
    }

    @Override
    public void registerScenes(@NotNull PonderSceneRegistrationHelper<ResourceLocation> helper) {
        Scenes.register(helper);
    }
}
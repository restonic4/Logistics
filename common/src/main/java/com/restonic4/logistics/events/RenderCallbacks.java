package com.restonic4.logistics.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.restonic4.logistics.events.core.Event;
import com.restonic4.logistics.events.core.EventFactory;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

import java.util.Map;

public class RenderCallbacks {
    @FunctionalInterface public interface OnHudRendered { void onEvent(GuiGraphics guiGraphics, float tickDelta); }
    public static final Event<OnHudRendered> ON_HUD_RENDERED = EventFactory.createVoid(OnHudRendered.class, callbacks -> (guiGraphics, tickDelta) -> {
        for (OnHudRendered callback : callbacks) {
            callback.onEvent(guiGraphics, tickDelta);
        }
    });

    @FunctionalInterface public interface OnLevelRendered { void onEvent(PoseStack poseStack, Camera camera); }
    public static final Event<OnLevelRendered> ON_LEVEL_RENDERED = EventFactory.createVoid(OnLevelRendered.class, callbacks -> (poseStack, camera) -> {
        for (OnLevelRendered callback : callbacks) {
            callback.onEvent(poseStack, camera);
        }
    });
}

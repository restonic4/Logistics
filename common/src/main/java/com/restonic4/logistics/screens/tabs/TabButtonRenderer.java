package com.restonic4.logistics.screens.tabs;

import net.minecraft.client.gui.GuiGraphics;

@FunctionalInterface
public interface TabButtonRenderer {
    void render(TabButton button, GuiGraphics gfx, int mouseX, int mouseY, float delta);
}
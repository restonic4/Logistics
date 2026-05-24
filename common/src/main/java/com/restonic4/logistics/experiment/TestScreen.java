package com.restonic4.logistics.experiment;

import com.restonic4.logistics.screens.widgets.ProgressBarWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TestScreen extends Screen {
    protected TestScreen() {
        super(Component.literal("Test Screen"));
    }

    @Override
    protected void init() {
        ProgressBarWidget bar = new ProgressBarWidget(0, 0, 300, 25, 50);
        bar.setRange(0, 100);
        bar.setBarColor(0xFFF7F7F7);
        this.addRenderableWidget(bar);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}

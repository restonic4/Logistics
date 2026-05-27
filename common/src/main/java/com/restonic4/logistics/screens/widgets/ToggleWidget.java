package com.restonic4.logistics.screens.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ToggleWidget extends AbstractWidget {
    private boolean value;
    private final Consumer<Boolean> onToggle;

    private static final int ON_COLOR = 0xFF55AA55;
    private static final int OFF_COLOR = 0xFFAA5555;
    private static final int TRACK_COLOR = 0xFF333333;
    private static final int THUMB_COLOR = 0xFFFFFFFF;

    public ToggleWidget(int x, int y, int width, int height, boolean initialValue, Consumer<Boolean> onToggle) {
        super(x, y, width, height, Component.empty());
        this.value = initialValue;
        this.onToggle = onToggle;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int trackColor = value ? ON_COLOR : OFF_COLOR;

        // Track (centered vertically, half height)
        int trackY = y + h / 4;
        int trackH = Math.max(2, h / 2);
        graphics.fill(x, trackY, x + w, trackY + trackH, trackColor);

        // Thumb (square, padded from edges)
        int thumbSize = Math.min(h - 4, w / 2 - 4);
        thumbSize = Math.max(thumbSize, 4);
        int thumbX = value ? (x + w - thumbSize - 2) : (x + 2);
        int thumbY = y + (h - thumbSize) / 2;

        // Thumb shadow/border for depth
        graphics.fill(thumbX - 1, thumbY - 1, thumbX + thumbSize + 1, thumbY + thumbSize + 1, 0xFF555555);
        graphics.fill(thumbX, thumbY, thumbX + thumbSize, thumbY + thumbSize, THUMB_COLOR);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.clicked(mouseX, mouseY)) {
            this.value = !this.value;
            if (onToggle != null) {
                onToggle.accept(value);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
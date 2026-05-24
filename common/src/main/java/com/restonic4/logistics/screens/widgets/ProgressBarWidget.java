package com.restonic4.logistics.screens.widgets;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.Consumer;

public class ProgressBarWidget extends AbstractWidget {
    private int value;
    private int minValue = Integer.MIN_VALUE;
    private int maxValue = Integer.MAX_VALUE;
    private int barColor = 0xFF00FF00;

    private static final int COLOR_BG = 0xFF000000;
    private static final int COLOR_BORDER = 0xFFA0A0A0;

    public ProgressBarWidget(int x, int y, int width, int height, int initialValue) {
        super(x, y, width, height, Component.empty());
        this.value = initialValue;
    }

    public void setRange(int min, int max) {
        this.minValue = min;
        this.maxValue = max;
        setValue(this.value);
    }

    public ProgressBarWidget setBarColor(int color) {
        this.barColor = color;
        return this;
    }

    public double getValue() {
        return value;
    }

    public double getProgress() {
        if (maxValue == minValue) return 0.0;
        return (double) (value - minValue) / (double) (maxValue - minValue);
    }

    public void setValue(int newValue) {
        int clamped = clamp(newValue);
        if (clamped == this.value) {
            return;
        }

        this.value = clamped;
    }

    public void setProgress(double progress) {
        setValue(minValue + (int) Math.round((maxValue - minValue) * Mth.clamp(progress, 0.0, 1.0)));
    }

    private int clamp(int val) {
        return Mth.clamp(val, minValue, maxValue);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();

        // Background
        graphics.fill(x, y, x + width, y + height, COLOR_BG);

        // Progress fill (inset 1px so it sits inside the border)
        int innerWidth = Math.max(0, width - 2);
        int fillWidth = (int) Math.round(innerWidth * getProgress());

        if (fillWidth > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + height - 1, barColor);
        }

        // Border on top
        graphics.renderOutline(x, y, width, height, COLOR_BORDER);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
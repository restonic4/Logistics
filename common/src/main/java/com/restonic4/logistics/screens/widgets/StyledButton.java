package com.restonic4.logistics.screens.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class StyledButton extends AbstractButton {
    // Defaults matching the panel chrome
    public static final int DEFAULT_BG = 0xFF161616;
    public static final int DEFAULT_BORDER = 0xFF2A2A2A;
    public static final int DEFAULT_TEXT = 0xFFFFFFFF;
    public static final int DEFAULT_HOVER_BG = 0xFF202020;
    public static final int DEFAULT_PRESS_BG = 0xFF252525;

    private int bgColor = DEFAULT_BG;
    private int borderColor = DEFAULT_BORDER;
    private int textColor = DEFAULT_TEXT;
    private int hoverBgColor = DEFAULT_HOVER_BG;
    private int pressBgColor = DEFAULT_PRESS_BG;

    private ResourceLocation icon;
    private int iconSize = 12;
    private boolean wasPressed = false;

    private final Runnable onPress;

    public StyledButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    public StyledButton withColors(int bg, int border, int text) {
        this.bgColor = bg;
        this.borderColor = border;
        this.textColor = text;
        return this;
    }

    public StyledButton withHoverColor(int hoverBg) {
        this.hoverBgColor = hoverBg;
        return this;
    }

    public StyledButton withPressColor(int pressBg) {
        this.pressBgColor = pressBg;
        return this;
    }

    public StyledButton withIcon(ResourceLocation icon, int size) {
        this.icon = icon;
        this.iconSize = size;
        return this;
    }

    public StyledButton withIcon(ResourceLocation icon) {
        return withIcon(icon, 12);
    }

    @Override
    public void onPress() {
        if (onPress != null) onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    @Override
    public void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        boolean pressed = this.isMouseOver(mouseX, mouseY) && this.isActive() && Minecraft.getInstance().mouseHandler.isLeftPressed();
        if (pressed && !wasPressed) {
            wasPressed = true;
        } else if (!Minecraft.getInstance().mouseHandler.isLeftPressed()) {
            wasPressed = false;
        }

        int bg;
        if (pressed && isMouseOver(mouseX, mouseY)) {
            bg = pressBgColor;
        } else if (this.isHovered()) {
            bg = hoverBgColor;
        } else {
            bg = bgColor;
        }

        int x = getX(), y = getY(), w = getWidth(), h = getHeight();

        // Background
        gfx.fill(x, y, x + w, y + h, bg);

        // 1px border on all four sides
        gfx.fill(x, y, x + w, y + 1, borderColor);         // top
        gfx.fill(x, y + h - 1, x + w, y + h, borderColor); // bottom
        gfx.fill(x, y, x + 1, y + h, borderColor);         // left
        gfx.fill(x + w - 1, y, x + w, y + h, borderColor); // right

        // Content: icon + text centered as a block
        Font font = Minecraft.getInstance().font;
        int textW = font.width(getMessage());
        int blockW = textW;
        if (icon != null) blockW += iconSize + 3;

        int drawX = x + (w - blockW) / 2;
        int textY = y + (h - 8) / 2;
        int iconY = y + (h - iconSize) / 2;

        if (icon != null) {
            gfx.blit(icon, drawX, iconY, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);
            drawX += iconSize + 3;
        }

        gfx.drawString(font, getMessage(), drawX, textY, textColor, false);
    }
}
package com.restonic4.logistics.screens.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class TabButton extends AbstractButton {
    private boolean selected;
    private ResourceLocation leftIcon;
    private ResourceLocation rightIcon;
    private int iconSize = 12;
    private TabButtonRenderer customRenderer;
    private final Runnable onPressCallback;

    // Style overrides (tweak per-instance if desired)
    public int colorSelectedBg = 0xFF252525;
    public int colorInactiveBg = 0xFF161616;
    public int colorInactiveHoverBg = 0xFF202020;
    public int colorBorder = 0xFF2A2A2A;
    public int colorSelectedTop = 0xFF3A3A3A;
    public int colorTextSelected = 0xFFFFFFFF;
    public int colorTextInactive = 0xFFAAAAAA;

    public TabButton(int x, int y, int width, int height, Component message, Runnable onPress, boolean selected) {
        super(x, y, width, height, message);
        this.onPressCallback = onPress;
        this.selected = selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setLeftIcon(ResourceLocation icon) {
        this.leftIcon = icon;
    }

    public void setRightIcon(ResourceLocation icon) {
        this.rightIcon = icon;
    }

    public void setIconSize(int size) {
        this.iconSize = size;
    }

    public void setCustomRenderer(TabButtonRenderer renderer) {
        this.customRenderer = renderer;
    }

    public ResourceLocation getLeftIcon() {
        return leftIcon;
    }

    public ResourceLocation getRightIcon() {
        return rightIcon;
    }

    public int getIconSize() {
        return iconSize;
    }

    @Override
    public void onPress() {
        if (onPressCallback != null) onPressCallback.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    @Override
    public void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        if (customRenderer != null) {
            customRenderer.render(this, gfx, mouseX, mouseY, delta);
        } else {
            renderDefault(gfx, mouseX, mouseY, delta);
        }

        // Render tooltip if hovered and in icon-only mode
        if (this.isHovered() && isIconOnlyMode()) {
            gfx.renderTooltip(Minecraft.getInstance().font, getMessage(), mouseX, mouseY);
        }
    }

    public boolean isIconOnlyMode() {
        return this.getWidth() < this.getHeight() * 2.5f;
    }

    public void renderDefault(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        int bg = selected ? colorSelectedBg : (this.isHovered() ? colorInactiveHoverBg : colorInactiveBg);
        int textColor = selected ? colorTextSelected : colorTextInactive;

        int x = getX(), y = getY(), w = getWidth(), h = getHeight();

        // Background
        gfx.fill(x, y, x + w, y + h, bg);

        // Borders: top, left, right always drawn
        gfx.fill(x, y, x + w, y + 1, colorBorder);
        gfx.fill(x, y, x + 1, y + h, colorBorder);
        gfx.fill(x + w - 1, y, x + w, y + h, colorBorder);

        // Selected tab connects to content: no bottom border, brighter top edge
        if (selected) {
            gfx.fill(x, y, x + w, y + 1, colorSelectedTop);
        } else {
            gfx.fill(x, y + h - 1, x + w, y + h, colorBorder);
        }

        Font font = Minecraft.getInstance().font;

        // Icon-only mode: width < height
        if (isIconOnlyMode()) {
            renderIconOnly(gfx, font, x, y, w, h, textColor);
            return;
        }

        // Normal mode: text + icons centered as a single block
        int textWidth = font.width(getMessage());

        int blockWidth = textWidth;
        if (leftIcon != null) blockWidth += iconSize + 2;
        if (rightIcon != null) blockWidth += iconSize + 2;

        int drawX = x + (w - blockWidth) / 2;
        int textY = y + (h - 8) / 2;

        if (leftIcon != null) {
            int iconY = y + (h - iconSize) / 2;
            gfx.blit(leftIcon, drawX, iconY, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);
            drawX += iconSize + 2;
        }

        gfx.drawString(font, getMessage(), drawX, textY, textColor, false);
        drawX += textWidth;

        if (rightIcon != null) {
            int iconY = y + (h - iconSize) / 2;
            gfx.blit(rightIcon, drawX + 2, iconY, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);
        }
    }

    private void renderIconOnly(GuiGraphics gfx, Font font, int x, int y, int w, int h, int textColor) {
        // Determine which icon to render (prefer left, then right)
        ResourceLocation iconToRender = leftIcon != null ? leftIcon : rightIcon;

        if (iconToRender != null) {
            // Center the icon
            int iconX = x + (w - iconSize) / 2;
            int iconY = y + (h - iconSize) / 2;
            gfx.blit(iconToRender, iconX, iconY, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);
        } else {
            // No icon available, render text centered (truncated if needed)
            String text = getMessage().getString();
            int textWidth = font.width(text);

            // If text is too wide, truncate with ellipsis
            if (textWidth > w - 4) {
                String ellipsis = "...";
                int ellipsisWidth = font.width(ellipsis);
                int maxTextWidth = w - 4 - ellipsisWidth;

                StringBuilder truncated = new StringBuilder();
                for (int i = 0; i < text.length(); i++) {
                    if (font.width(truncated.toString() + text.charAt(i)) > maxTextWidth) {
                        break;
                    }
                    truncated.append(text.charAt(i));
                }
                text = truncated.toString() + ellipsis;
                textWidth = font.width(text);
            }

            int textX = x + (w - textWidth) / 2;
            int textY = y + (h - 8) / 2;
            gfx.drawString(font, text, textX, textY, textColor, false);
        }
    }
}
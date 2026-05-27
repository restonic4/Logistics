package com.restonic4.logistics.screens.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class PlayerChipWidget extends AbstractWidget {
    private final UUID playerId;
    private final String username;
    private final Runnable onRemove;

    private static final int BG = 0xFF252525;
    private static final int BORDER = 0xFF3A3A3A;
    private static final int REMOVE_HOVER = 0xFFFF5555;

    public PlayerChipWidget(int x, int y, int width, int height, UUID playerId, String username, Runnable onRemove) {
        super(x, y, width, height, Component.literal(username));
        this.playerId = playerId;
        this.username = username;
        this.onRemove = onRemove;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        // Background
        graphics.fill(x, y, x + w, y + h, BG);
        graphics.renderOutline(x, y, w, h, BORDER);

        // Head
        PlayerHeadRenderer.renderHead(graphics, username, playerId, x + 4, y + (h - 16) / 2, 16);

        // Name
        Font font = Minecraft.getInstance().font;
        String text = username;
        int maxWidth = w - 32;
        if (font.width(text) > maxWidth) {
            text = font.plainSubstrByWidth(text, maxWidth - font.width("...")) + "...";
        }
        graphics.drawString(font, text, x + 24, y + (h - 8) / 2, 0xFFFFFFFF, false);

        // Remove button (× symbol)
        boolean removeHovered = mouseX >= x + w - 18 && mouseX < x + w - 2 && mouseY >= y + 2 && mouseY < y + h - 2;
        int removeColor = removeHovered ? REMOVE_HOVER : 0xFFAAAAAA;
        String xSymbol = "×";
        int rx = x + w - 16;
        int ry = y + (h - 10) / 2;
        graphics.drawString(font, xSymbol, rx, ry, removeColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.clicked(mouseX, mouseY)) return false;

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        if (mouseX >= x + w - 18 && mouseX < x + w - 2 && mouseY >= y + 2 && mouseY < y + h - 2) {
            if (onRemove != null) {
                onRemove.run();
            }
            return true;
        }

        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
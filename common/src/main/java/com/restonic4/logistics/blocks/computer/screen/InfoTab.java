package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.screens.tabs.Tab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class InfoTab extends Tab {
    public InfoTab() {
        super(Component.literal("Info"));
    }

    @Override
    public void init(Screen parent, int x, int y, int width, int height) {

    }

    @Override
    public void onHide() {

    }

    @Override
    public void tick() {

    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta, int x, int y, int width, int height) {
        Font font = Minecraft.getInstance().font;

        // Configuration layout boundaries
        int paddingX = 15;
        int currentX = x + paddingX;
        int currentY = y + 15;
        int contentWidth = width - (paddingX * 2);

        // --- RENDER CONTENT ---

        // Main Title (H1) with a Chest Block Icon
        currentY = renderH1(gfx, font, Component.literal("Logistics"), new ItemStack(BlockRegistry.COMPUTER_BLOCK.getBlock()), currentX, currentY);
        currentY += 4;

        // Horizontal Separator Line
        currentY = renderHorizontalLine(gfx, currentX, currentY, contentWidth, 0x44FFFFFF);
        currentY += 10;

        // Regular Paragraph Body Text
        Component body1 = Component.translatable("screen.logistics.computer.info_body");
        currentY = renderParagraph(gfx, font, body1, currentX, currentY, contentWidth, 0xFFCCCCCC);
        currentY += 12;

    }

    private int renderH1(GuiGraphics gfx, Font font, Component text, ItemStack icon, int x, int y) {
        return renderHeading(gfx, font, text, icon, x, y, 1.5f, 0xFFFFFFFF);
    }

    private int renderH2(GuiGraphics gfx, Font font, Component text, ItemStack icon, int x, int y) {
        return renderHeading(gfx, font, text, icon, x, y, 1.2f, 0xFFE0E0E0);
    }

    private int renderHeading(GuiGraphics gfx, Font font, Component text, ItemStack icon, int x, int y, float scale, int color) {
        int iconSize = 16;
        int textXOffset = x;

        // 1. Draw the Icon if it exists
        if (icon != null && !icon.isEmpty()) {
            // Vertically center the icon alongside the scaled text line height
            int iconY = y + (int) (((font.lineHeight * scale) - iconSize) / 2);
            gfx.renderFakeItem(icon, textXOffset, iconY);
            textXOffset += iconSize + 6; // Move text to the right of the icon + small gap
        }

        // 2. Scale and Draw Text using Matrix Transformations
        gfx.pose().pushPose();
        // Translate right to target position so our local scaling doesn't push the position out of bounds
        gfx.pose().translate(textXOffset, y, 0);
        gfx.pose().scale(scale, scale, 1.0f);

        // Coordinates are 0,0 because we handled placement via translate above
        gfx.drawString(font, text, 0, 0, color, true);
        gfx.pose().popPose();

        // Calculate height footprint taken by this entire heading layout element
        int totalHeight = Math.max((int) (font.lineHeight * scale), icon != null && !icon.isEmpty() ? iconSize : 0);
        return y + totalHeight + 6; // Returns next available progressive Y line position (includes 6px margin)
    }

    private int renderParagraph(GuiGraphics gfx, Font font, Component text, int x, int y, int wrapWidth, int color) {
        List<FormattedCharSequence> splitLines = font.split(text, wrapWidth);
        for (net.minecraft.util.FormattedCharSequence line : splitLines) {
            gfx.drawString(font, line, x, y, color, false);
            y += font.lineHeight + 2; // Line spacing adjustments
        }
        return y;
    }

    private int renderHorizontalLine(GuiGraphics gfx, int x, int y, int width, int color) {
        int thickness = 1;
        gfx.fill(x, y, x + width, y + thickness, color);
        return y + thickness;
    }
}
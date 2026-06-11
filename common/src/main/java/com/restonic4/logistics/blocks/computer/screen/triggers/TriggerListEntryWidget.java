package com.restonic4.logistics.blocks.computer.screen.triggers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * One row of the trigger list on the Triggers tab: trigger name plus a short config
 * summary, a selected/hover highlight, and a small delete hit zone on the right.
 */
public class TriggerListEntryWidget extends AbstractWidget {
    private static final int SELECTED_BG = 0x33FFFFFF;
    private static final int HOVER_BG = 0x22FFFFFF;
    private static final int DELETE_ZONE = 16;

    private final String title;
    private final String summary;
    private final Runnable onSelect;
    private final Runnable onDelete;

    private boolean selected;

    public TriggerListEntryWidget(
            int x, int y, int width, int height, String title, String summary,
            Runnable onSelect, Runnable onDelete
    ) {
        super(x, y, width, height, Component.literal(title));
        this.title = title;
        this.summary = summary;
        this.onSelect = onSelect;
        this.onDelete = onDelete;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();

        if (selected) {
            gfx.fill(x, y, x + w, y + h, SELECTED_BG);
        } else if (isMouseOver(mouseX, mouseY)) {
            gfx.fill(x, y, x + w, y + h, HOVER_BG);
        }

        Font font = Minecraft.getInstance().font;
        int textX = x + 4;
        gfx.drawString(font, title, textX, y + 3, 0xFFFFFFFF, false);
        if (summary != null && !summary.isEmpty()) {
            String clipped = font.plainSubstrByWidth(summary, w - DELETE_ZONE - 8);
            gfx.drawString(font, clipped, textX, y + h - 11, 0xFF888888, false);
        }

        boolean overDelete = isOverDelete(mouseX, mouseY);
        gfx.drawString(font, "✕", x + w - DELETE_ZONE + 4, y + (h - 8) / 2,
                overDelete ? 0xFFFF5555 : 0xFF666666, false);
    }

    private boolean isOverDelete(double mouseX, double mouseY) {
        return isMouseOver(mouseX, mouseY) && mouseX >= getX() + getWidth() - DELETE_ZONE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || !isMouseOver(mouseX, mouseY) || button != 0) return false;

        if (isOverDelete(mouseX, mouseY)) {
            if (onDelete != null) onDelete.run();
        } else {
            if (onSelect != null) onSelect.run();
        }
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}

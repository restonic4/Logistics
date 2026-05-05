package com.restonic4.logistics.screens;

import com.restonic4.logistics.networking.NetworkTooltipPayload;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class NetworkScannerOverlay {
    private static List<Component> activeRows = List.of();
    private static List<Boolean> activeIsLine = List.of();
    private static boolean areCreateGogglesPresent = false;

    private static final int PAD_X = 8;
    private static final int PAD_Y = 6;
    private static final int ROW_H = 10;
    private static final int LINE_H = 5;
    private static final int MARGIN_SIDE = 8;
    private static final int MARGIN_CENTER_GAP = 16;

    private static final int COLOR_BG_FILL = 0xD0101010;
    private static final int COLOR_BG_BORDER = 0xFF2A2A2A;
    private static final int COLOR_LINE = 0xFF404040;

    public static void setActiveTooltip(NetworkTooltipPayload payload) {
        activeRows = payload.getRows();
        activeIsLine = payload.getIsLine();
        areCreateGogglesPresent = payload.areCreateGogglesPresent();
    }

    public static void register() {
        HudRenderCallback.EVENT.register(NetworkScannerOverlay::onHudRender);
    }

    private static void onHudRender(GuiGraphics graphics, float tickDelta) {
        if (activeRows.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int centerX = sw / 2;

        // Panel dimensions
        int contentWidth = 0;
        for (int i = 0; i < activeRows.size(); i++) {
            if (Boolean.TRUE.equals(activeIsLine.get(i))) continue;
            int w = font.width(activeRows.get(i));
            if (w > contentWidth) contentWidth = w;
        }

        int panelW = contentWidth + PAD_X * 2;

        int totalH = PAD_Y * 2;
        for (int i = 0; i < activeRows.size(); i++) {
            totalH += Boolean.TRUE.equals(activeIsLine.get(i)) ? LINE_H : ROW_H;
        }

        int panelRight;
        int panelLeft;
        if (!areCreateGogglesPresent) {
            panelRight = sw - MARGIN_SIDE;
            panelLeft = panelRight - panelW;

            if (panelLeft < centerX + MARGIN_CENTER_GAP) {
                panelLeft = centerX + MARGIN_CENTER_GAP;
                panelRight = panelLeft + panelW;
            }
        } else {
            panelLeft = MARGIN_SIDE;
            panelRight = panelLeft + panelW;

            if (panelRight > centerX - MARGIN_CENTER_GAP) {
                panelRight = centerX - MARGIN_CENTER_GAP;
                panelLeft = panelRight - panelW;
            }
        }

        int panelTop = (sh - totalH) / 2;
        int panelBottom = panelTop + totalH;

        // Draw
        drawPanel(graphics, panelLeft, panelTop, panelRight, panelBottom);

        int cursor = panelTop + PAD_Y;

        for (int i = 0; i < activeRows.size(); i++) {
            if (Boolean.TRUE.equals(activeIsLine.get(i))) {
                // Horizontal separator line
                int lineY = cursor + LINE_H / 2;
                graphics.fill(
                        panelLeft + PAD_X / 2,
                        lineY,
                        panelRight - PAD_X / 2,
                        lineY + 1,
                        COLOR_LINE
                );
                cursor += LINE_H;
            } else {
                graphics.drawString(
                        font,
                        activeRows.get(i),
                        panelLeft + PAD_X,
                        cursor,
                        0xFFFFFFFF,
                        true // drop shadow
                );
                cursor += ROW_H;
            }
        }
    }

    private static void drawPanel(GuiGraphics g, int x0, int y0, int x1, int y1) {
        // Fill
        g.fill(x0, y0, x1, y1, COLOR_BG_FILL);

        // 1-px border
        g.fill(x0, y0, x1, y0 + 1, COLOR_BG_BORDER); // top
        g.fill(x0, y1 - 1,  x1, y1, COLOR_BG_BORDER); // bottom
        g.fill(x0, y0, x0 + 1, y1, COLOR_BG_BORDER); // left
        g.fill(x1 - 1,  y0, x1, y1, COLOR_BG_BORDER); // right
    }
}

package com.restonic4.logistics.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Abstract base class for overlay {@link Screen}s that present information in
 * one or more floating panels.
 *
 * <h2>Subclassing</h2>
 * <ol>
 *   <li>Call {@code super(title)} or {@code super(title, theme)} from your
 *       constructor.</li>
 *   <li>Implement {@link #buildPanels()} — collect your data, build
 *       {@link Panel} objects using the fluent builder, and return them in
 *       left-to-right display order.</li>
 *   <li>Optionally override {@link #onRenderBackground(GuiGraphics, int, int,
 *       float)} for a custom backdrop, or {@link #onRenderForeground(GuiGraphics,
 *       int, int, float)} to draw anything on top of the panels.</li>
 * </ol>
 *
 * <h2>Layout</h2>
 * Panels are placed left-to-right with {@code pad} spacing.  If the next panel
 * would overflow the screen width it is wrapped onto a new row below.  Each
 * panel is height-clamped so it never overflows the screen bottom; any clipped
 * {@link Panel.ListEntry} automatically shows a {@code "… N more"} trailer.
 *
 * <h2>Theme</h2>
 * Every colour and layout metric is defined in a {@link PanelScreenTheme}.
 * Pass a custom one to the constructor, or use {@link PanelScreenTheme#DEFAULT}.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public class MyInfoScreen extends PanelScreen {
 *
 *     public MyInfoScreen() {
 *         super(Component.literal("My Info"));
 *     }
 *
 *     @Override
 *     protected List<Panel> buildPanels() {
 *         Panel p = Panel.create("STATS")
 *                 .row("Players online", String.valueOf(count), theme.colGood)
 *                 .gap()
 *                 .bar("Load", used, max, (float) used / max);
 *         return List.of(p);
 *     }
 * }
 * }</pre>
 */
public abstract class PanelScreen extends Screen {

    // -------------------------------------------------------------------------
    // Theme — accessible to subclasses (read-only by convention)
    // -------------------------------------------------------------------------

    /** The active theme. Subclasses may read colour and metric values from it. */
    protected final PanelScreenTheme theme;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Creates a panel screen with the default dark-green theme.
     *
     * @param title Title stored in the {@link Screen} superclass (not rendered
     *              visibly by this class, but used by accessibility APIs and
     *              the vanilla back-button).
     */
    protected PanelScreen(Component title) {
        this(title, PanelScreenTheme.DEFAULT);
    }

    /**
     * Creates a panel screen with a custom theme.
     *
     * @param title Title stored in the {@link Screen} superclass.
     * @param theme Theme instance; use {@link PanelScreenTheme.Builder} to
     *              create one, or {@link PanelScreenTheme.Builder#from} to
     *              derive from an existing theme.
     */
    protected PanelScreen(Component title, PanelScreenTheme theme) {
        super(title);
        this.theme = theme;
    }

    // -------------------------------------------------------------------------
    // Core contract
    // -------------------------------------------------------------------------

    /**
     * Called once per frame, before panels are rendered.
     *
     * <p>Gather your data here and return the panels you want displayed.
     * Panels are laid out left-to-right; the layout engine wraps to a new row
     * when needed.
     *
     * <p><strong>Performance note:</strong> this runs every render tick.
     * Avoid expensive operations or cache results between frames as needed.
     *
     * @return Ordered list of panels to display; never {@code null}, but may
     *         be empty (nothing will be drawn).
     */
    protected abstract List<Panel> buildPanels();

    // -------------------------------------------------------------------------
    // Hooks — optional overrides
    // -------------------------------------------------------------------------

    /**
     * Called at the very beginning of {@link #render}, before any panels or
     * widgets are drawn.  Override to render a custom background.
     *
     * <p>The default implementation does nothing (transparent game world
     * background).
     */
    protected void onRenderBackground(GuiGraphics gfx, int mouseX, int mouseY,
                                      float partialTick) {}

    /**
     * Called after all panels and the dismiss hint have been drawn.  Override
     * to render additional foreground elements (e.g. a tooltip, a title bar).
     */
    protected void onRenderForeground(GuiGraphics gfx, int mouseX, int mouseY,
                                      float partialTick) {}

    // -------------------------------------------------------------------------
    // Screen overrides
    // -------------------------------------------------------------------------

    /** The screen does not pause the game by default. Override to change. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // GLFW_KEY_ESCAPE (256) — close the screen
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // -------------------------------------------------------------------------
    // Render — layout engine
    // -------------------------------------------------------------------------

    @Override
    public final void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        onRenderBackground(gfx, mouseX, mouseY, partialTick);

        List<Panel> panels = buildPanels();

        int cursorX = theme.pad;
        int cursorY = theme.pad;
        int rowMaxH = 0; // tallest panel on the current row, for wrapping

        for (Panel panel : panels) {
            int panelH = computePanelHeight(panel);

            // Clamp so the panel never overflows the screen bottom
            int availH  = this.height - cursorY - theme.pad;
            int clampedH = Math.min(panelH, availH);

            // Wrap to new row if this panel would overflow the right edge
            if (cursorX + theme.panelWidth > this.width && cursorX > theme.pad) {
                cursorY += rowMaxH + theme.pad;
                cursorX  = theme.pad;
                rowMaxH  = 0;
                // Re-clamp for the new row position
                availH   = this.height - cursorY - theme.pad;
                clampedH = Math.min(panelH, availH);
            }

            drawPanel(gfx, cursorX, cursorY, theme.panelWidth, clampedH);

            RenderContext ctx = new RenderContext(
                    theme,
                    cursorX + theme.pad,
                    cursorY + theme.pad,
                    clampedH - theme.pad * 2,
                    this
            );
            renderPanelContent(gfx, ctx, panel);

            rowMaxH  = Math.max(rowMaxH, clampedH);
            cursorX += theme.panelWidth + theme.pad;
        }

        // Dismiss hint — bottom-left
        gfx.drawString(
                Minecraft.getInstance().font,
                "[ESC] Close",
                theme.pad,
                this.height - theme.pad - theme.lineHeight,
                theme.colHint,
                false
        );

        onRenderForeground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    // -------------------------------------------------------------------------
    // Panel content renderer
    // -------------------------------------------------------------------------

    /**
     * Renders all entries in a panel in order.
     * Each entry-type delegates to a dedicated draw method.
     */
    private void renderPanelContent(GuiGraphics gfx, RenderContext ctx, Panel panel) {
        // Section header + underline is always the first thing in a panel
        ctx.y = drawSectionHeader(gfx, ctx.contentX, ctx.y, panel.title);

        for (Panel.Entry entry : panel.entries) {
            if (entry instanceof Panel.HeaderEntry e) {
                ctx.y = drawSectionHeader(gfx, ctx.contentX, ctx.y, e.text());
            } else if (entry instanceof Panel.RowEntry e) {
                ctx.y = drawTwoColRow(gfx, ctx.contentX, ctx.y, e.label(), e.value(),
                        e.valueColour() == -1 ? theme.colValue : e.valueColour());
            } else if (entry instanceof Panel.BarEntry e) {
                ctx.y = drawProgressBar(gfx, ctx.contentX, ctx.y, e.label(),
                        e.pct(), e.valueText());
            } else if (entry instanceof Panel.ListEntry e) {
                ctx.y = drawClippedList(gfx, ctx.contentX, ctx.y, e.header(),
                        e.items(),
                        e.itemColour() == -1 ? theme.colValue : e.itemColour(),
                        ctx.bottomBound());
            } else if (entry instanceof Panel.GapEntry) {
                ctx.y += theme.sectionGap;
            } else if (entry instanceof Panel.DividerEntry) {
                ctx.y = drawDivider(gfx, ctx.contentX, ctx.y);
            } else if (entry instanceof Panel.CustomEntry e) {
                ctx.y = e.renderer().apply(gfx, ctx);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Height pre-calculation
    // -------------------------------------------------------------------------

    /**
     * Estimates the pixel height that a panel will require, including padding.
     * Used for layout and clamping — does not need to be pixel-perfect.
     */
    private int computePanelHeight(Panel panel) {
        int rows     = 0;
        int extraPx  = 0; // non-lineHeight pixels (bar pixels, gaps)

        // Title header always rendered: text row + sectionGap
        rows    += 1;
        extraPx += theme.sectionGap;

        for (Panel.Entry entry : panel.entries) {
            if (entry instanceof Panel.HeaderEntry) {
                rows    += 1;
                extraPx += theme.sectionGap;
            } else if (entry instanceof Panel.RowEntry) {
                rows += 1;
            } else if (entry instanceof Panel.BarEntry) {
                rows    += 1; // label row
                extraPx += 6 + 2; // barH + bottom margin
            } else if (entry instanceof Panel.ListEntry e) {
                if (e.header() != null) rows += 1;
                rows += e.items().size();
            } else if (entry instanceof Panel.GapEntry) {
                extraPx += theme.sectionGap;
            } else if (entry instanceof Panel.DividerEntry) {
                rows += 1;
            } else if (entry instanceof Panel.CustomEntry e) {
                rows += e.heightRows();
            }
        }

        return theme.pad * 2 + rows * theme.lineHeight + extraPx;
    }

    // -------------------------------------------------------------------------
    // Drawing primitives — also exposed via RenderContext to custom entries
    // -------------------------------------------------------------------------

    /**
     * Draws the panel chrome: drop shadow, filled background, border, and the
     * accent top stripe.
     */
    void drawPanel(GuiGraphics gfx, int x, int y, int w, int h) {
        // Drop shadow
        gfx.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x60000000);
        // Background
        gfx.fill(x, y, x + w, y + h, theme.colBackground);
        // Border (four single-pixel edges)
        gfx.fill(x,         y,         x + w,     y + 1,     theme.colBorder);
        gfx.fill(x,         y + h - 1, x + w,     y + h,     theme.colBorder);
        gfx.fill(x,         y,         x + 1,     y + h,     theme.colBorder);
        gfx.fill(x + w - 1, y,         x + w,     y + h,     theme.colBorder);
        // Accent top stripe (inside the border)
        gfx.fill(x + 1, y + 1, x + w - 1, y + 2, theme.colAccent);
    }

    /**
     * Draws a section-header text with an accent underline.
     *
     * @return Updated {@code y} after the header and its underline gap.
     */
    int drawSectionHeader(GuiGraphics gfx, int x, int y, String text) {
        var font = Minecraft.getInstance().font;
        gfx.drawString(font, text, x, y, theme.colHeader, false);
        gfx.fill(x, y + theme.lineHeight, x + font.width(text) + 2,
                y + theme.lineHeight + 1, theme.colAccent);
        return y + theme.lineHeight + theme.sectionGap;
    }

    /**
     * Draws a label/value row in two columns.
     *
     * @param valueColour Explicit ARGB colour for the value text.
     * @return Updated {@code y} after the row.
     */
    int drawTwoColRow(GuiGraphics gfx, int x, int y,
                      String label, String value, int valueColour) {
        var font = Minecraft.getInstance().font;
        gfx.drawString(font, label, x,                         y, theme.colLabel, false);
        gfx.drawString(font, value, x + theme.col2X - theme.pad, y, valueColour,   false);
        return y + theme.lineHeight;
    }

    /**
     * Draws a labelled horizontal progress bar.
     *
     * <p>The bar colour is determined by comparing {@code pct} against
     * {@link PanelScreenTheme#barHighThreshold} and
     * {@link PanelScreenTheme#barLowThreshold}.
     *
     * @param label     Label text shown in the left column.
     * @param pct       Fill fraction {@code [0f, 1f]}.
     * @param valueText Optional string shown in the right column; if
     *                  {@code null} the column is left empty.
     * @return Updated {@code y} after the bar.
     */
    int drawProgressBar(GuiGraphics gfx, int x, int y,
                        String label, float pct, String valueText) {
        var font  = Minecraft.getInstance().font;
        int barW  = theme.panelWidth - theme.pad * 2;
        int barH  = 6;

        // Label row
        gfx.drawString(font, label, x, y, theme.colLabel, false);
        if (valueText != null) {
            gfx.drawString(font, valueText, x + theme.col2X - theme.pad, y,
                    theme.colValue, false);
        }
        y += theme.lineHeight;

        // Background track
        gfx.fill(x, y, x + barW, y + barH, theme.colBarTrack);

        // Fill
        int fillCol = pct > theme.barHighThreshold ? theme.colBarHigh
                : pct > theme.barLowThreshold  ? theme.colBarMid
                : theme.colBarLow;
        int fillW = Math.max(0, (int) (barW * Math.min(1f, pct)));
        if (fillW > 0) gfx.fill(x, y, x + fillW, y + barH, fillCol);

        // Percentage label centred on bar
        String pctStr = String.format("%.1f%%", pct * 100f);
        int textX = x + barW / 2 - font.width(pctStr) / 2;
        gfx.drawString(font, pctStr, textX, y - 1, theme.colHeader, false);

        return y + barH + 2;
    }

    /**
     * Draws a one-pixel horizontal divider line at the full content width.
     *
     * @return Updated {@code y} after the divider.
     */
    int drawDivider(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y, x + theme.panelWidth - theme.pad * 2, y + 1, theme.colDivider);
        return y + theme.lineHeight;
    }

    /**
     * Draws a header and a list of strings, clipping gracefully when the
     * available vertical space is exhausted.
     *
     * <p>When items are clipped the last visible line is replaced with
     * {@code "  … N more"}.
     *
     * @param header      Optional header text; pass {@code null} to omit.
     * @param items       Items to display.
     * @param itemColour  ARGB colour for each item string.
     * @param bottomBound Pixel Y below which nothing should be drawn (i.e.
     *                    {@code panelY + panelH - pad}).
     * @return Updated {@code y} after the list.
     */
    int drawClippedList(GuiGraphics gfx, int x, int y,
                        String header, List<String> items,
                        int itemColour, int bottomBound) {
        var font = Minecraft.getInstance().font;

        if (header != null && y + theme.lineHeight <= bottomBound) {
            gfx.drawString(font, header, x, y, theme.colLabel, false);
            y += theme.lineHeight;
        }

        for (int i = 0; i < items.size(); i++) {
            if (y + theme.lineHeight > bottomBound - theme.lineHeight) {
                int remaining = items.size() - i;
                if (remaining > 0) {
                    gfx.drawString(font, "  … " + remaining + " more",
                            x, y, theme.colLabel, false);
                    y += theme.lineHeight;
                }
                break;
            }
            gfx.drawString(font, items.get(i), x, y, itemColour, false);
            y += theme.lineHeight;
        }

        return y;
    }

    // -------------------------------------------------------------------------
    // Utilities — available to subclasses
    // -------------------------------------------------------------------------

    /** Strips Minecraft {@code §X} colour codes from a string. */
    protected static String stripFormatting(String s) {
        return s.replaceAll("§.", "");
    }

    // -------------------------------------------------------------------------
    // RenderContext — passed to custom entries
    // -------------------------------------------------------------------------

    /**
     * Snapshot of the render state at a specific entry within a panel,
     * passed to {@link Panel.CustomEntry} lambdas.
     *
     * <p>The {@code y} field is mutable; the custom renderer must both read
     * and write it (return the updated value from its lambda).
     */
    public static final class RenderContext {

        /** The active theme. */
        public final PanelScreenTheme theme;

        /** Left edge of the panel's content area ({@code panelX + pad}). */
        public final int contentX;

        /**
         * Current vertical cursor.  Updated by each entry renderer; the
         * custom lambda receives it at the start of its entry and must return
         * the new value.
         */
        public int y;

        /**
         * Total height available to content inside this panel, in pixels
         * ({@code clampedPanelHeight - pad * 2}).
         */
        public final int contentHeight;

        /**
         * Absolute pixel Y of the first content row (i.e. {@code panelY + pad}).
         * Fixed for the lifetime of this context.
         */
        private final int startY;

        /** Back-reference to the owning screen (for font, width, etc.). */
        public final PanelScreen screen;

        RenderContext(PanelScreenTheme theme, int contentX, int startY,
                      int contentHeight, PanelScreen screen) {
            this.theme         = theme;
            this.contentX      = contentX;
            this.y             = startY;
            this.startY        = startY;
            this.contentHeight = contentHeight;
            this.screen        = screen;
        }

        /**
         * The absolute pixel Y below which nothing should be drawn inside this
         * panel ({@code panelY + pad + contentHeight}).
         *
         * <p>This value is constant throughout the panel render — it does not
         * move as {@link #y} advances.  Pass it directly to
         * {@link PanelScreen#drawClippedList} as the {@code bottomBound} argument.
         */
        public int bottomBound() {
            return startY + contentHeight;
        }

        // ---- Proxy helpers so custom entries don't need a cast ------------

        /** @see PanelScreen#drawTwoColRow */
        public int row(GuiGraphics gfx, String label, String value, int colour) {
            return screen.drawTwoColRow(gfx, contentX, y, label, value, colour);
        }

        /** @see PanelScreen#drawProgressBar */
        public int bar(GuiGraphics gfx, String label, float pct, String valueText) {
            return screen.drawProgressBar(gfx, contentX, y, label, pct, valueText);
        }

        /** @see PanelScreen#drawSectionHeader */
        public int header(GuiGraphics gfx, String text) {
            return screen.drawSectionHeader(gfx, contentX, y, text);
        }

        /** @see PanelScreen#drawDivider */
        public int divider(GuiGraphics gfx) {
            return screen.drawDivider(gfx, contentX, y);
        }

        /** @see PanelScreen#drawClippedList */
        public int list(GuiGraphics gfx, String header, List<String> items,
                        int itemColour, int bottomBound) {
            return screen.drawClippedList(gfx, contentX, y, header, items,
                    itemColour, bottomBound);
        }
    }
}
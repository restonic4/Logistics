package com.restonic4.logistics.screens;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Descriptor for a single panel rendered by {@link PanelScreen}.
 *
 * <p>Build one via {@link Panel#create(String)} then chain row-adding calls.
 * The panel's height is computed automatically from its content, so you never
 * have to calculate it yourself.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Override
 * protected List<Panel> buildPanels() {
 *     Panel stats = Panel.create("WORLD STATS")
 *             .row("Networks",   String.valueOf(nets),   theme.colValue)
 *             .row("Game tick",  String.valueOf(tick),   theme.colNeutral)
 *             .gap()
 *             .bar("Buffer", buffer, maxBuffer, pct)
 *             .list("Nodes", nodeLines, theme.colValue);
 *
 *     return List.of(stats);
 * }
 * }</pre>
 */
public final class Panel {

    // -------------------------------------------------------------------------
    // Entry types
    // -------------------------------------------------------------------------

    /**
     * A single logical row inside a panel.
     *
     * <p>Subtypes are sealed via the {@code type} field; the renderer switches
     * on it, keeping all drawing code inside {@link PanelScreen}.
     */
    public sealed interface Entry
            permits Panel.HeaderEntry,
            Panel.RowEntry,
            Panel.BarEntry,
            Panel.ListEntry,
            Panel.GapEntry,
            Panel.DividerEntry,
            Panel.CustomEntry {}

    /** A large section-header with an accent underline. */
    public record HeaderEntry(String text) implements Entry {}

    /**
     * A two-column label / value row.
     *
     * @param valueColour ARGB colour for the value text; pass {@code -1} to
     *                    use the theme's default {@code colValue}.
     */
    public record RowEntry(String label, String value, int valueColour) implements Entry {}

    /**
     * A labelled progress bar.
     *
     * @param label      Label shown above-left of the bar.
     * @param current    Current fill value.
     * @param max        Maximum value (bar is clamped to {@code [0, max]}).
     * @param pct        Pre-computed fill fraction {@code [0f, 1f]}.
     * @param valueText  Text shown in the right column of the label row (may
     *                   be {@code null} to omit).
     */
    public record BarEntry(String label, long current, long max, float pct,
                           String valueText) implements Entry {}

    /**
     * A vertically clipped list of strings.
     *
     * <p>The renderer will automatically append {@code "… N more"} if the
     * available panel height is exhausted before all items are drawn.
     *
     * @param header     Small header text shown above the list (may be
     *                   {@code null} to omit).
     * @param items      Ordered list of display strings.
     * @param itemColour ARGB colour for each item; pass {@code -1} to use
     *                   the theme's {@code colValue}.
     */
    public record ListEntry(String header, List<String> items,
                            int itemColour) implements Entry {}

    /** An empty vertical gap (one {@code sectionGap} unit). */
    public record GapEntry() implements Entry {}

    /** A one-pixel horizontal divider line. */
    public record DividerEntry() implements Entry {}

    /**
     * An escape hatch for fully custom rendering.
     *
     * <p>Provide a lambda that receives {@code (GuiGraphics gfx, RenderContext ctx)}
     * and returns the new {@code y} after rendering.
     * {@link PanelScreen.RenderContext} exposes the theme, fonts, current
     * content position, and all drawing helpers so you can mix custom content
     * seamlessly with built-in rows.
     *
     * @param heightRows Approximate row-count used for panel-height pre-
     *                   calculation (does not need to be exact; err high).
     * @param renderer   Lambda {@code (gfx, ctx) -> newY}.
     */
    public record CustomEntry(
            int heightRows,
            BiFunction<GuiGraphics, PanelScreen.RenderContext, Integer> renderer
    ) implements Entry {}

    // -------------------------------------------------------------------------
    // Panel fields
    // -------------------------------------------------------------------------

    /** Title shown in the panel header (rendered by the layout engine). */
    public final String title;

    /**
     * Ordered content entries.
     * Treated as read-only once the panel is handed to {@link PanelScreen}.
     */
    public final List<Entry> entries;

    // -------------------------------------------------------------------------
    // Factory / builder
    // -------------------------------------------------------------------------

    private Panel(String title) {
        this.title   = title;
        this.entries = new ArrayList<>();
    }

    /**
     * Creates a new panel with the given title.
     *
     * @param title Section-header text, e.g. {@code "LEVEL / WORLD"}.
     */
    public static Panel create(String title) {
        return new Panel(title);
    }

    // ---- fluent row adders --------------------------------------------------

    /**
     * Adds a two-column row using the theme's default {@code colValue} for the
     * value text.
     */
    public Panel row(String label, String value) {
        entries.add(new RowEntry(label, value, -1));
        return this;
    }

    /**
     * Adds a two-column row with an explicit value colour.
     *
     * @param valueColour ARGB colour, e.g. {@code theme.colGood}.
     */
    public Panel row(String label, String value, int valueColour) {
        entries.add(new RowEntry(label, value, valueColour));
        return this;
    }

    /**
     * Adds a labelled progress bar.
     *
     * @param label     Label text.
     * @param current   Current numeric value (used for display string only).
     * @param max       Maximum numeric value (used for display string only).
     * @param pct       Fill fraction {@code [0f, 1f]}.
     * @param valueText Optional right-column text (e.g. {@code "17.3kEU / 50kEU"});
     *                  pass {@code null} to omit.
     */
    public Panel bar(String label, long current, long max, float pct, String valueText) {
        entries.add(new BarEntry(label, current, max, pct, valueText));
        return this;
    }

    /**
     * Adds a labelled progress bar without a pre-formatted value string.
     * The renderer will show {@code current / max} using the theme's default
     * formatting.
     */
    public Panel bar(String label, long current, long max, float pct) {
        return bar(label, current, max, pct, null);
    }

    /**
     * Adds a vertically clipped list of strings using the theme's default
     * {@code colValue}.
     *
     * @param header Optional small header above the list; pass {@code null} to
     *               omit.
     * @param items  Items to display.
     */
    public Panel list(String header, List<String> items) {
        entries.add(new ListEntry(header, List.copyOf(items), -1));
        return this;
    }

    /**
     * Adds a vertically clipped list with an explicit item colour.
     */
    public Panel list(String header, List<String> items, int itemColour) {
        entries.add(new ListEntry(header, List.copyOf(items), itemColour));
        return this;
    }

    /** Adds a blank vertical gap (one {@code sectionGap} unit tall). */
    public Panel gap() {
        entries.add(new GapEntry());
        return this;
    }

    /** Adds a one-pixel horizontal divider line. */
    public Panel divider() {
        entries.add(new DividerEntry());
        return this;
    }

    /**
     * Adds a fully custom-rendered entry.
     *
     * <p>The {@code renderer} lambda receives a {@link PanelScreen.RenderContext}
     * that exposes the theme, the Minecraft font, the current content X/Y
     * origin, and all of {@link PanelScreen}'s drawing helpers.  Return the new
     * {@code y} value after drawing.
     *
     * @param heightRows Row-count estimate used for height pre-calculation.
     * @param renderer   {@code (gfx, ctx) -> newY}.
     */
    public Panel custom(int heightRows,
                        BiFunction<GuiGraphics, PanelScreen.RenderContext, Integer> renderer) {
        entries.add(new CustomEntry(heightRows, renderer));
        return this;
    }
}
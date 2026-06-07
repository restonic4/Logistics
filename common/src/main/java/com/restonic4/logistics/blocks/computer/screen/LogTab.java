package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.blocks.computer.ComputerLogEntry;
import com.restonic4.logistics.blocks.computer.ComputerLogger;
import com.restonic4.logistics.screens.tabs.Tab;
import com.restonic4.logistics.screens.widgets.StyledButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class LogTab extends Tab {

    // ── State ─────────────────────────────────────────────────────────────────

    /** All entries received this session (full sync + pushes). */
    private final List<ComputerLogEntry> entries = new ArrayList<>();

    /** Currently active severity filter; null = show all. */
    private ComputerLogEntry.Severity filter = null;

    /** Pixel offset from the top of the log content. */
    private double scrollY = 0;

    /**
     * Whether the view is "pinned" to the bottom so new entries scroll into
     * view automatically.  Becomes false when the user scrolls up manually.
     */
    private boolean autoScroll = true;

    // ── Layout geometry (set in init) ─────────────────────────────────────────

    private int tabX, tabY, tabW, tabH;

    private static final int FILTER_BAR_HEIGHT = 18;
    private static final int ROW_HEIGHT        = 12;   // px per log entry
    private static final int BADGE_WIDTH       = 3;    // left colour stripe
    private static final int LEFT_PAD          = 6;
    private static final int RIGHT_PAD         = 6;
    private static final int SCROLLBAR_WIDTH   = 4;

    // Colours
    private static final int COL_BG_HEADER = 0xFF111111;
    private static final int COL_BG_EVEN   = 0xFF161616;
    private static final int COL_BG_ODD    = 0xFF1C1C1C;
    private static final int COL_TIMESTAMP = 0xFF888888;
    private static final int COL_MESSAGE   = 0xFFDDDDDD;
    private static final int COL_SCROLLBAR = 0xFF555555;
    private static final int COL_SCROLLBAR_HOVER = 0xFF888888;

    private static final float TEXT_SCALE = 0.75f;

    // ── Constructor ───────────────────────────────────────────────────────────

    public LogTab() {
        super(Component.translatable("screen.logistics.computer.tab.logs.title"));
    }

    // ── Tab lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void init(Screen parent, int x, int y, int width, int height) {
        this.tabX = x;
        this.tabY = y;
        this.tabW = width;
        this.tabH = height;

        // Filter buttons
        int btnY  = y + 2;
        int btnH  = FILTER_BAR_HEIGHT - 4;
        int btnW  = 32;
        int gap   = 2;
        int startX = x + LEFT_PAD;

        addFilterButton(parent, startX, btnY, btnW, btnH, Component.translatable("screen.logistics.computer.tab.logs.all").getString(), null);
        addFilterButton(parent, startX + (btnW + gap), btnY, btnW, btnH, Component.translatable("screen.logistics.computer.tab.logs.info").getString(), ComputerLogEntry.Severity.INFO);
        addFilterButton(parent, startX + (btnW + gap) * 2,btnY, btnW, btnH, Component.translatable("screen.logistics.computer.tab.logs.warn").getString(), ComputerLogEntry.Severity.WARN);
        addFilterButton(parent, startX + (btnW + gap) * 3,btnY, btnW, btnH, Component.translatable("screen.logistics.computer.tab.logs.error").getString(), ComputerLogEntry.Severity.ERROR);
    }

    private void addFilterButton(Screen parent, int x, int y, int w, int h, String label, ComputerLogEntry.Severity severity) {
        StyledButton btn = new StyledButton(x, y, w, h,
                Component.literal(label),
                () -> {
                    this.filter = severity;
                    this.scrollY = 0;
                    this.autoScroll = true;
                }
        );
        styleFilterButton(btn, severity);
        parent.addRenderableWidget(btn);
    }

    private void styleFilterButton(StyledButton btn, ComputerLogEntry.Severity sev) {
        int accent = sev == null ? 0xFF2A2A2A
                : sev == ComputerLogEntry.Severity.INFO  ? 0xFF1A3050
                : sev == ComputerLogEntry.Severity.WARN  ? 0xFF503010
                : /* ERROR */ 0xFF501010;

        btn.withColors(accent, 0xFF3A3A3A, 0xFFFFFFFF)
                .withHoverColor(brighten(accent))
                .withPressColor(0xFF303030);
    }

    private static int brighten(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 24);
        int g = Math.min(255, ((argb >>  8) & 0xFF) + 24);
        int b = Math.min(255, ( argb        & 0xFF) + 24);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void onHide() {}

    @Override
    public void tick() {}

    // ── Incoming data ─────────────────────────────────────────────────────────

    /** Called once when the screen opens to populate the full history. */
    public void receiveFullSync(List<ComputerLogEntry> incoming) {
        entries.clear();
        entries.addAll(incoming);
        autoScroll = true;
        clampScroll();
    }

    /** Called for each new entry while the screen is open. */
    public void receivePush(ComputerLogEntry entry) {
        entries.add(entry);
        if (entries.size() > ComputerLogger.MAX_ENTRIES) {
            entries.remove(0);
        }
        if (autoScroll) {
            scrollToBottom();
        }
    }



    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta,
                       int x, int y, int width, int height) {
        // Store geometry in case init() hasn't been called yet (shouldn't happen)
        this.tabX = x; this.tabY = y; this.tabW = width; this.tabH = height;

        Font font = Minecraft.getInstance().font;

        // ── Filter bar background ──────────────────────────────────────────────
        gfx.fill(x, y, x + width, y + FILTER_BAR_HEIGHT, COL_BG_HEADER);

        // ── Log content area ──────────────────────────────────────────────────
        int listTop    = y + FILTER_BAR_HEIGHT;
        int listBottom = y + height;
        int listHeight = listBottom - listTop;
        int listWidth  = width - SCROLLBAR_WIDTH - 2;

        List<ComputerLogEntry> visible = filteredEntries();

        // Compute scaled row height
        float scale  = TEXT_SCALE;
        int scaledRow = (int) Math.ceil(ROW_HEIGHT * scale); // visual row height after scaling
        int contentH  = visible.size() * scaledRow;

        // Clamp scroll
        double maxScroll = Math.max(0, contentH - listHeight);
        scrollY = Mth.clamp(scrollY, 0, maxScroll);

        gfx.enableScissor(x, listTop, x + width, listBottom);

        for (int i = 0; i < visible.size(); i++) {
            ComputerLogEntry entry = visible.get(i);

            int rowY = listTop + (int) (i * scaledRow - scrollY);
            if (rowY + scaledRow < listTop || rowY > listBottom) continue;

            // Row background
            int rowBg = (i % 2 == 0) ? COL_BG_EVEN : COL_BG_ODD;
            gfx.fill(x, rowY, x + listWidth, rowY + scaledRow, rowBg);

            // Severity badge (left stripe)
            gfx.fill(x, rowY, x + BADGE_WIDTH, rowY + scaledRow, entry.severity().color);

            // Text (scaled down)
            gfx.pose().pushPose();
            gfx.pose().translate(x + LEFT_PAD, rowY, 0);
            gfx.pose().scale(scale, scale, 1f);

            int curX = 0;

            // Timestamp
            String ts = entry.formattedTime();
            gfx.drawString(font, ts, curX, 0, COL_TIMESTAMP, false);
            curX += (int) ((font.width(ts) + 4));

            // Severity prefix
            gfx.drawString(font, entry.severity().prefix, curX, 0, entry.severity().color, false);
            curX += (int) (font.width(entry.severity().prefix) + 2);

            // Message — clip if too wide
            int maxMsgWidth = (int) ((listWidth - LEFT_PAD - curX) / scale);
            String msg = entry.message();
            if (font.width(msg) > maxMsgWidth) {
                msg = font.plainSubstrByWidth(msg, maxMsgWidth - font.width("...")) + "...";
            }
            gfx.drawString(font, msg, curX, 0, COL_MESSAGE, false);

            gfx.pose().popPose();
        }

        gfx.disableScissor();

        // ── Scrollbar ─────────────────────────────────────────────────────────
        if (contentH > listHeight && listHeight > 0) {
            int sbX    = x + width - SCROLLBAR_WIDTH - 1;
            int sbH    = Math.max(16, (int) ((double) listHeight / contentH * listHeight));
            int sbMaxY = listHeight - sbH;
            int sbY    = listTop + (int) (scrollY / maxScroll * sbMaxY);

            boolean sbHovered = mouseX >= sbX && mouseX <= sbX + SCROLLBAR_WIDTH
                    && mouseY >= sbY && mouseY <= sbY + sbH;

            gfx.fill(sbX, listTop, sbX + SCROLLBAR_WIDTH, listBottom, 0xFF0A0A0A);
            gfx.fill(sbX, sbY, sbX + SCROLLBAR_WIDTH, sbY + sbH,
                    sbHovered ? COL_SCROLLBAR_HOVER : COL_SCROLLBAR);
        }

        // ── Empty state ───────────────────────────────────────────────────────
        if (visible.isEmpty()) {
            String msg = Component.translatable(filter == null ? "screen.logistics.computer.tab.logs.no_logs" : "screen.logistics.computer.tab.logs.no_logs_here").getString();
            int tw = (int) (font.width(msg) * 0.9f);
            int tx = x + (width - tw) / 2;
            int ty = listTop + listHeight / 2 - 4;

            gfx.pose().pushPose();
            gfx.pose().translate(tx, ty, 0);
            gfx.pose().scale(0.9f, 0.9f, 1f);
            gfx.drawString(font, msg, 0, 0, 0xFF555555, false);
            gfx.pose().popPose();
        }
    }

    // ── Mouse / scroll input ──────────────────────────────────────────────────

    /**
     * The tab itself has no widgets that consume scroll events — the tab bar
     * calls through to the active tab's render, but not its input.  We hook
     * into the owning Screen via {@link #onShow()} / {@link #onHide()} by
     * relying on the screen forwarding {@code mouseScrolled}.
     *
     * Instead, we expose a method that {@link ComputerScreen} can call from
     * its own {@code mouseScrolled} override when this tab is active.
     */
    public boolean handleMouseScrolled(double mouseX, double mouseY, double amount,
                                       int x, int y, int width, int height) {
        int listTop    = y + FILTER_BAR_HEIGHT;
        int listBottom = y + height;

        if (mouseX < x || mouseX > x + width || mouseY < listTop || mouseY > listBottom) {
            return false;
        }

        double prevScroll = scrollY;
        scrollY -= amount * ROW_HEIGHT * 3;
        clampScroll();

        // If the user scrolled up, disable auto-scroll so new entries don't jump them
        if (scrollY < prevScroll) {
            autoScroll = false;
        }
        // If they scrolled all the way to the bottom, re-enable auto-scroll
        if (isAtBottom()) {
            autoScroll = true;
        }

        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<ComputerLogEntry> filteredEntries() {
        if (filter == null) return entries;
        List<ComputerLogEntry> result = new ArrayList<>();
        for (ComputerLogEntry e : entries) {
            if (e.severity() == filter) result.add(e);
        }
        return result;
    }

    private void scrollToBottom() {
        // Set to a very large value; clampScroll() will bring it to the real max
        scrollY = Double.MAX_VALUE / 2;
        clampScroll();
    }

    private void clampScroll() {
        float scale    = TEXT_SCALE;
        int scaledRow  = (int) Math.ceil(ROW_HEIGHT * scale);
        int contentH   = filteredEntries().size() * scaledRow;
        int listHeight = tabH - FILTER_BAR_HEIGHT;
        double maxScroll = Math.max(0, contentH - listHeight);
        scrollY = Mth.clamp(scrollY, 0, maxScroll);
    }

    private boolean isAtBottom() {
        float scale    = TEXT_SCALE;
        int scaledRow  = (int) Math.ceil(ROW_HEIGHT * scale);
        int contentH   = filteredEntries().size() * scaledRow;
        int listHeight = tabH - FILTER_BAR_HEIGHT;
        double maxScroll = Math.max(0, contentH - listHeight);
        return scrollY >= maxScroll - 1;
    }
}
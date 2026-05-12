package com.restonic4.logistics.screens.tabs;

import com.restonic4.logistics.screens.widgets.StyledButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public abstract class TabbedScreen extends Screen {
    protected final List<Tab> tabs = new ArrayList<>();
    protected int selectedTab = -1;

    // Geometry
    protected int panelLeft, panelTop, panelRight, panelBottom;
    protected int contentLeft, contentTop, contentRight, contentBottom;
    protected int panelWidth = 388;
    protected int panelHeight = 240;
    protected int tabBarHeight = 24;

    // Tab bar configuration
    protected int tabBarLeftReserve = 0;
    protected int tabBarRightReserve = 0;
    protected int tabGap = 2;
    protected TabDistribution distribution = TabDistribution.CENTER;
    protected boolean tabCompact = false;
    protected int tabFixedWidth = 80;
    protected int tabMinWidth = 40;
    protected int tabMaxWidth = 160;

    // Pagination
    protected int tabPageIndex = 0;
    protected static final int ARROW_WIDTH = 14;
    protected static final int ARROW_MARGIN = 2;

    // FILL mode specific
    protected static final int FILL_MAX_TABS_PER_PAGE = 4;

    // Colors
    protected int colorPanelBg = 0xFF121212;
    protected int colorContentBg = 0xFF1A1A1A;
    protected int colorBorder = 0xFF2A2A2A;

    protected TabbedScreen(Component title) {
        super(title);
    }

    protected void addTab(Tab tab) {
        tabs.add(tab);
    }

    @Override
    protected void init() {
        calculateBounds();
        if (selectedTab < 0 && !tabs.isEmpty()) selectedTab = 0;
        selectTab(selectedTab >= 0 ? selectedTab : 0);
    }

    protected void calculateBounds() {
        panelLeft = (this.width - panelWidth) / 2;
        panelRight = panelLeft + panelWidth;
        panelTop = (this.height - panelHeight) / 2;
        panelBottom = panelTop + panelHeight;

        contentLeft = panelLeft + 1;
        contentTop = panelTop + tabBarHeight;
        contentRight = panelRight - 1;
        contentBottom = panelBottom - 1;
    }

    protected void selectTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        int prev = selectedTab;
        if (prev >= 0 && prev < tabs.size()) tabs.get(prev).onHide();

        this.clearWidgets();
        selectedTab = index;

        // Jump to the page that contains the newly selected tab
        tabPageIndex = computePageForTab(index);

        buildTabButtons();
        addPersistentWidgets();

        Tab tab = tabs.get(selectedTab);
        tab.init(this, contentLeft, contentTop, contentRight - contentLeft, contentBottom - contentTop);
        tab.onShow();
    }

    /** Rebuilds tab bar + current tab widgets without changing the selected tab. */
    protected void rebuildTabBar() {
        if (selectedTab >= 0 && selectedTab < tabs.size()) {
            tabs.get(selectedTab).onHide();
        }
        this.clearWidgets();
        buildTabButtons();
        addPersistentWidgets();

        if (selectedTab >= 0 && selectedTab < tabs.size()) {
            Tab tab = tabs.get(selectedTab);
            tab.init(this, contentLeft, contentTop, contentRight - contentLeft, contentBottom - contentTop);
            tab.onShow();
        }
    }

    private int[] measureTabs(Font font) {
        int[] widths = new int[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            if (tabCompact) {
                int w = font.width(tab.getTitle()) + 10;
                if (tab.getLeftIcon() != null) w += 14;
                if (tab.getRightIcon() != null) w += 14;
                widths[i] = Math.max(tabMinWidth, Math.min(tabMaxWidth, w));
            } else {
                widths[i] = Math.max(tabMinWidth, Math.min(tabMaxWidth, tabFixedWidth));
            }
        }
        return widths;
    }

    private record Page(int start, int end) {}

    /**
     * Standard left-to-right pagination for LEFT.
     * Arrows consume space from the tab area as needed.
     */
    private List<Page> computePagesStandard(int[] widths, int barStart, int barEnd) {
        List<Page> pages = new ArrayList<>();
        int arrowSpace = ARROW_WIDTH + tabGap;
        int i = 0;
        while (i < tabs.size()) {
            int start = i;
            int x = barStart + (start > 0 ? arrowSpace : 0);
            while (i < tabs.size()) {
                int gap = (i > start) ? tabGap : 0;
                int w = widths[i];
                boolean hasMore = i < tabs.size() - 1;
                int tail = hasMore ? arrowSpace : 0;
                if (x + gap + w + tail > barEnd && i > start) break;
                x += gap + w;
                i++;
            }
            pages.add(new Page(start, i));
        }
        return pages;
    }

    /**
     * Center pagination: always reserve space for both arrows so tabs can be centered
     * in the full width with arrows sitting at the edges of the tab block.
     */
    private List<Page> computePagesCenter(int[] widths, int barStart, int barEnd) {
        List<Page> pages = new ArrayList<>();
        int arrowSpace = ARROW_WIDTH + tabGap;
        int i = 0;
        while (i < tabs.size()) {
            int start = i;
            // Always reserve both arrow spaces so centered tabs never overlap arrows
            int x = barStart + arrowSpace;
            while (i < tabs.size()) {
                int gap = (i > start) ? tabGap : 0;
                int w = widths[i];
                boolean hasMore = i < tabs.size() - 1;
                int tail = hasMore ? arrowSpace : 0;
                if (x + gap + w + tail > barEnd && i > start) break;
                x += gap + w;
                i++;
            }
            pages.add(new Page(start, i));
        }
        return pages;
    }

    /**
     * RIGHT mode pagination: standard left-to-right, but tabs are right-aligned visually.
     * Same page computation as LEFT.
     */
    private List<Page> computePagesRight(int[] widths, int barStart, int barEnd) {
        // Same as standard left-to-right, but we will right-align the tab block later
        return computePagesStandard(widths, barStart, barEnd);
    }

    /**
     * FILL mode pagination: strictly by count, up to FILL_MAX_TABS_PER_PAGE per page.
     * Widths are adjusted later to fit, so we only care about tab count here.
     */
    private List<Page> computePagesFill(int[] widths, int barStart, int barEnd) {
        List<Page> pages = new ArrayList<>();
        int i = 0;
        while (i < tabs.size()) {
            int start = i;
            int end = Math.min(i + FILL_MAX_TABS_PER_PAGE, tabs.size());
            pages.add(new Page(start, end));
            i = end;
        }
        return pages;
    }

    private int computePageForTab(int tabIndex) {
        if (tabs.isEmpty()) return 0;
        Font font = Minecraft.getInstance().font;
        int margin = 2;
        int effectiveRightReserve = Math.max(0, tabBarRightReserve);
        int barStart = panelLeft + margin + tabBarLeftReserve;
        int barEnd = panelRight - margin - effectiveRightReserve;
        int[] widths = measureTabs(font);
        List<Page> pages;
        if (distribution == TabDistribution.FILL) {
            pages = computePagesFill(widths, barStart, barEnd);
        } else if (distribution == TabDistribution.CENTER) {
            pages = computePagesCenter(widths, barStart, barEnd);
        } else if (distribution == TabDistribution.RIGHT) {
            pages = computePagesRight(widths, barStart, barEnd);
        } else if (distribution == TabDistribution.FILL_NO_PAGES) {
            return 0;
        } else {
            pages = computePagesStandard(widths, barStart, barEnd);
        }
        for (int p = 0; p < pages.size(); p++) {
            Page page = pages.get(p);
            if (tabIndex >= page.start && tabIndex < page.end) return p;
        }
        return 0;
    }

    protected void buildTabButtons() {
        if (tabs.isEmpty()) return;

        Font font = Minecraft.getInstance().font;
        int margin = 2;
        int effectiveRightReserve = Math.max(0, tabBarRightReserve);
        int barStart = panelLeft + margin + tabBarLeftReserve;
        int barEnd = panelRight - margin - effectiveRightReserve;
        int available = Math.max(0, barEnd - barStart);

        int[] widths = measureTabs(font);

        int totalWidth = 0;
        for (int w : widths) totalWidth += w;
        totalWidth += (tabs.size() - 1) * tabGap;

        // FILL_NO_PAGES: always fit all tabs, no pagination, no arrows
        if (distribution == TabDistribution.FILL_NO_PAGES) {
            buildFillNoPagesLayout(widths, barStart, barEnd, available);
            return;
        }

        // Check if everything fits on one page (no overflow)
        if (totalWidth <= available && distribution != TabDistribution.FILL) {
            buildNoOverflowLayout(widths, barStart, barEnd, available);
            return;
        }

        // Overflow or FILL mode: use pagination
        buildPaginatedLayout(widths, barStart, barEnd, available);
    }

    /** Layout when all tabs fit on one page (no overflow, no arrows needed). */
    private void buildNoOverflowLayout(int[] widths, int barStart, int barEnd, int available) {
        tabPageIndex = 0;

        if (distribution == TabDistribution.FILL && tabs.size() > 0) {
            int totalGap = (tabs.size() - 1) * tabGap;
            int base = (available - totalGap) / tabs.size();
            int rem = (available - totalGap) % tabs.size();
            for (int i = 0; i < tabs.size(); i++) {
                widths[i] = base + (i < rem ? 1 : 0);
            }
        }

        int visibleTotal = 0;
        for (int w : widths) visibleTotal += w;
        visibleTotal += (tabs.size() - 1) * tabGap;

        int startX;
        switch (distribution) {
            case LEFT -> startX = barStart;
            case RIGHT -> startX = Math.max(barStart, barEnd - visibleTotal);
            case CENTER -> startX = barStart + Math.max(0, (available - visibleTotal) / 2);
            case FILL -> startX = barStart;
            default -> startX = barStart;
        }

        int tabY = panelTop + 2;
        int baseH = contentTop - tabY - 1;
        int selectedH = baseH + 1;

        for (int i = 0; i < tabs.size(); i++) {
            final int idx = i;
            int x = startX;
            for (int j = 0; j < i; j++) x += widths[j] + tabGap;

            int h = (i == selectedTab) ? selectedH : baseH;
            TabButton btn = new TabButton(x, tabY, widths[i], h, tabs.get(i).getTitle(), () -> selectTab(idx), i == selectedTab);

            Tab tab = tabs.get(i);
            if (tab.getLeftIcon() != null) btn.setLeftIcon(tab.getLeftIcon());
            if (tab.getRightIcon() != null) btn.setRightIcon(tab.getRightIcon());
            if (tab.getCustomButtonRenderer() != null) btn.setCustomRenderer(tab.getCustomButtonRenderer());

            this.addRenderableWidget(btn);
        }
    }

    /** FILL_NO_PAGES: fit all tabs, expand to fill available space, no min width enforcement. */
    private void buildFillNoPagesLayout(int[] widths, int barStart, int barEnd, int available) {
        tabPageIndex = 0;

        if (tabs.size() > 0) {
            int totalGap = (tabs.size() - 1) * tabGap;
            int base = (available - totalGap) / tabs.size();
            int rem = (available - totalGap) % tabs.size();
            for (int i = 0; i < tabs.size(); i++) {
                widths[i] = base + (i < rem ? 1 : 0);
            }
        }

        int tabY = panelTop + 2;
        int baseH = contentTop - tabY - 1;
        int selectedH = baseH + 1;

        int x = barStart;
        for (int i = 0; i < tabs.size(); i++) {
            final int idx = i;
            if (i > 0) x += tabGap;
            int w = widths[i];
            int h = (i == selectedTab) ? selectedH : baseH;
            TabButton btn = new TabButton(x, tabY, w, h, tabs.get(i).getTitle(), () -> selectTab(idx), i == selectedTab);

            Tab tab = tabs.get(i);
            if (tab.getLeftIcon() != null) btn.setLeftIcon(tab.getLeftIcon());
            if (tab.getRightIcon() != null) btn.setRightIcon(tab.getRightIcon());
            if (tab.getCustomButtonRenderer() != null) btn.setCustomRenderer(tab.getCustomButtonRenderer());

            this.addRenderableWidget(btn);
            x += w;
        }
    }

    /** Paginated layout with arrows. */
    private void buildPaginatedLayout(int[] widths, int barStart, int barEnd, int available) {
        int arrowSpace = ARROW_WIDTH + tabGap;
        List<Page> pages;
        boolean isRight = distribution == TabDistribution.RIGHT;

        if (distribution == TabDistribution.FILL) {
            pages = computePagesFill(widths, barStart, barEnd);
        } else if (distribution == TabDistribution.CENTER) {
            pages = computePagesCenter(widths, barStart, barEnd);
        } else if (isRight) {
            pages = computePagesRight(widths, barStart, barEnd);
        } else {
            pages = computePagesStandard(widths, barStart, barEnd);
        }

        tabPageIndex = Math.max(0, Math.min(tabPageIndex, pages.size() - 1));

        Page page = pages.get(tabPageIndex);

        // Arrow presence: standard logic for all modes
        boolean showLeft = tabPageIndex > 0;
        boolean showRight = tabPageIndex < pages.size() - 1;

        int tabY = panelTop + 2;
        int baseH = contentTop - tabY - 1;
        int selectedH = baseH + 1;

        // Calculate visible tab width total for this page
        int visibleTotal = 0;
        for (int i = page.start; i < page.end; i++) {
            visibleTotal += widths[i];
        }
        visibleTotal += (page.end - page.start - 1) * tabGap;

        // FILL: expand tabs to fill the content area (between arrows if present)
        if (distribution == TabDistribution.FILL) {
            int tabCount = page.end - page.start;
            int totalGap = (tabCount - 1) * tabGap;
            int contentAvailable = available - (showLeft ? arrowSpace : 0) - (showRight ? arrowSpace : 0);
            contentAvailable = Math.max(contentAvailable, tabCount * tabMinWidth + totalGap);

            if (tabCount > 0) {
                int baseWidth = (contentAvailable - totalGap) / tabCount;
                int rem = (contentAvailable - totalGap) % tabCount;
                for (int i = page.start; i < page.end; i++) {
                    widths[i] = Math.max(tabMinWidth, baseWidth + (i - page.start < rem ? 1 : 0));
                }
            }

            // Recalculate visibleTotal after expansion
            visibleTotal = 0;
            for (int i = page.start; i < page.end; i++) {
                visibleTotal += widths[i];
            }
            visibleTotal += totalGap;
        }

        // Determine content bounds (area between arrows if present)
        int contentStartX = barStart + (showLeft ? arrowSpace : 0);
        int contentEndX = barEnd - (showRight ? arrowSpace : 0);

        // Determine tab block starting X based on distribution
        int startX;
        if (distribution == TabDistribution.CENTER) {
            // Center in the FULL available width — arrows do NOT affect centering
            startX = barStart + Math.max(0, (available - visibleTotal) / 2);
        } else if (isRight) {
            // Right-align within content area
            startX = Math.max(contentStartX, contentEndX - visibleTotal);
        } else {
            // LEFT, FILL: start at content left edge
            startX = contentStartX;
        }

        int tabBlockStart = startX;
        int tabBlockEnd = startX + visibleTotal;

        // =====================================================================
        // Place arrows according to distribution rules
        // =====================================================================

        if (distribution == TabDistribution.CENTER) {
            // CENTER: arrows at the tips of the tab block, not panel corners
            if (showLeft) {
                int arrowX = tabBlockStart - arrowSpace;
                StyledButton left = new StyledButton(arrowX, tabY, ARROW_WIDTH, baseH, Component.literal("<"), () -> {
                    tabPageIndex = Math.max(0, tabPageIndex - 1);
                    rebuildTabBar();
                });
                styleArrow(left);
                this.addRenderableWidget(left);
            }
            if (showRight) {
                int arrowX = tabBlockEnd + tabGap;
                final int totalPages = pages.size();
                StyledButton right = new StyledButton(arrowX, tabY, ARROW_WIDTH, baseH, Component.literal(">"), () -> {
                    tabPageIndex = Math.min(totalPages - 1, tabPageIndex + 1);
                    rebuildTabBar();
                });
                styleArrow(right);
                this.addRenderableWidget(right);
            }
        } else if (isRight) {
            // RIGHT: right arrow at panel edge (hidden on last page)
            // left arrow adjacent to tab block on its left side
            if (showRight) {
                int arrowX = barEnd - ARROW_WIDTH;
                final int totalPages = pages.size();
                StyledButton right = new StyledButton(arrowX, tabY, ARROW_WIDTH, baseH, Component.literal(">"), () -> {
                    tabPageIndex = Math.min(totalPages - 1, tabPageIndex + 1);
                    rebuildTabBar();
                });
                styleArrow(right);
                this.addRenderableWidget(right);
            }
            if (showLeft) {
                int arrowX = tabBlockStart - arrowSpace;
                StyledButton left = new StyledButton(arrowX, tabY, ARROW_WIDTH, baseH, Component.literal("<"), () -> {
                    tabPageIndex = Math.max(0, tabPageIndex - 1);
                    rebuildTabBar();
                });
                styleArrow(left);
                this.addRenderableWidget(left);
            }
        } else {
            // LEFT / FILL: left arrow at panel edge, right arrow adjacent to tab block
            if (showLeft) {
                StyledButton left = new StyledButton(barStart, tabY, ARROW_WIDTH, baseH, Component.literal("<"), () -> {
                    tabPageIndex = Math.max(0, tabPageIndex - 1);
                    rebuildTabBar();
                });
                styleArrow(left);
                this.addRenderableWidget(left);
            }
            if (showRight) {
                int arrowX = tabBlockEnd + tabGap;
                final int totalPages = pages.size();
                StyledButton right = new StyledButton(arrowX, tabY, ARROW_WIDTH, baseH, Component.literal(">"), () -> {
                    tabPageIndex = Math.min(totalPages - 1, tabPageIndex + 1);
                    rebuildTabBar();
                });
                styleArrow(right);
                this.addRenderableWidget(right);
            }
        }

        // =====================================================================
        // Place tabs
        // =====================================================================
        for (int i = page.start; i < page.end; i++) {
            int idx = i;
            int w = widths[i];
            int h = (i == selectedTab) ? selectedH : baseH;
            int x = startX + getPageTabOffset(widths, page.start, i);

            TabButton btn = new TabButton(x, tabY, w, h, tabs.get(i).getTitle(), () -> selectTab(idx), i == selectedTab);

            Tab tab = tabs.get(i);
            if (tab.getLeftIcon() != null) btn.setLeftIcon(tab.getLeftIcon());
            if (tab.getRightIcon() != null) btn.setRightIcon(tab.getRightIcon());
            if (tab.getCustomButtonRenderer() != null) btn.setCustomRenderer(tab.getCustomButtonRenderer());

            this.addRenderableWidget(btn);
        }
    }

    private int getPageTabOffset(int[] widths, int pageStart, int targetIndex) {
        int offset = 0;
        for (int i = pageStart; i < targetIndex; i++) {
            offset += widths[i] + tabGap;
        }
        return offset;
    }

    private void styleArrow(StyledButton arrow) {
        arrow.withColors(0xFF161616, 0xFF2A2A2A, 0xFFAAAAAA)
                .withHoverColor(0xFF202020)
                .withPressColor(0xFF252525);
    }

    protected void addPersistentWidgets() {}

    @Override
    public void tick() {
        super.tick();
        if (selectedTab >= 0 && selectedTab < tabs.size()) {
            tabs.get(selectedTab).tick();
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        this.renderBackground(gfx);

        gfx.fill(panelLeft, panelTop, panelRight, panelBottom, colorPanelBg);
        gfx.fill(contentLeft, contentTop, contentRight, contentBottom, colorContentBg);

        gfx.fill(panelLeft, panelTop, panelRight, panelTop + 1, colorBorder);
        gfx.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, colorBorder);
        gfx.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, colorBorder);
        gfx.fill(panelRight - 1, panelTop, panelRight, panelBottom, colorBorder);

        gfx.fill(contentLeft, contentTop, contentRight, contentTop + 1, colorBorder);

        if (selectedTab >= 0 && selectedTab < tabs.size()) {
            Tab tab = tabs.get(selectedTab);
            tab.render(gfx, mouseX, mouseY, delta, contentLeft, contentTop, contentRight - contentLeft, contentBottom - contentTop);
        }

        super.render(gfx, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
package com.restonic4.logistics.screens.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class ScrollablePanel extends AbstractWidget {
    private final List<AbstractWidget> children = new ArrayList<>();
    private final List<int[]> relativePositions = new ArrayList<>(); // [relX, relY]
    private int scrollOffset = 0;
    private int padding = 6;
    private int contentHeight = 0;
    private int bgColor = 0xFF1A1A1A;
    private int scrollbarColor = 0xFFAAAAAA;

    public ScrollablePanel(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public void setPadding(int padding) {
        this.padding = padding;
        refreshPositions();
    }

    public void setBgColor(int color) {
        this.bgColor = color;
    }

    public void addChild(AbstractWidget child, int relX, int relY) {
        children.add(child);
        relativePositions.add(new int[]{relX, relY});
        refreshChildPosition(children.size() - 1);
        updateContentHeight();
    }

    public void removeChild(AbstractWidget child) {
        int index = children.indexOf(child);
        if (index >= 0) {
            children.remove(index);
            relativePositions.remove(index);
            updateContentHeight();
        }
    }

    public void clearChildren() {
        children.clear();
        relativePositions.clear();
        scrollOffset = 0;
        contentHeight = 0;
    }

    public List<AbstractWidget> getChildren() {
        return new ArrayList<>(children);
    }

    private void refreshChildPosition(int index) {
        AbstractWidget child = children.get(index);
        int[] rel = relativePositions.get(index);
        child.setX(getX() + padding + rel[0]);
        child.setY(getY() + padding + rel[1] - scrollOffset);
    }

    public void refreshPositions() {
        for (int i = 0; i < children.size(); i++) {
            refreshChildPosition(i);
        }
    }

    public void setScrollOffset(int offset) {
        int maxScroll = Math.max(0, contentHeight - (height - padding * 2));
        this.scrollOffset = Mth.clamp(offset, 0, maxScroll);
        refreshPositions();
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public int getContentHeight() {
        return contentHeight;
    }

    public int getPadding() {
        return padding;
    }

    private void updateContentHeight() {
        int max = 0;
        for (int i = 0; i < children.size(); i++) {
            int[] rel = relativePositions.get(i);
            int bottom = rel[1] + children.get(i).getHeight();
            if (bottom > max) max = bottom;
        }
        this.contentHeight = max;
        // Re-clamp scroll
        setScrollOffset(scrollOffset);
    }

    @Override
    public void setX(int x) {
        int dx = x - getX();
        super.setX(x);
        for (AbstractWidget child : children) {
            child.setX(child.getX() + dx);
        }
    }

    @Override
    public void setY(int y) {
        int dy = y - getY();
        super.setY(y);
        for (AbstractWidget child : children) {
            child.setY(child.getY() + dy);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = width;
        int h = height;

        // Background
        graphics.fill(x, y, x + w, y + h, bgColor);

        int contentX = x + padding;
        int contentY = y + padding;
        int contentW = w - padding * 2;
        int contentH = h - padding * 2;

        // Scissor to content area
        graphics.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);

        // Render children
        for (AbstractWidget child : children) {
            if (child.visible && isChildInView(child, contentY, contentH)) {
                child.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        graphics.disableScissor();

        // Render expanded dropdown menus on top of scissor
        for (AbstractWidget child : children) {
            if (child instanceof SearchableDropdownWidget<?> dropdown && dropdown.isExpanded()) {
                dropdown.renderMenuOverlay(graphics, mouseX, mouseY, partialTick);
            }
        }

        // Scrollbar
        int maxScroll = Math.max(0, contentHeight - contentH);
        if (maxScroll > 0) {
            int scrollbarHeight = Math.max(10, (int) ((double) contentH / contentHeight * contentH));
            int scrollbarY = contentY + (int) ((double) scrollOffset / maxScroll * (contentH - scrollbarHeight));
            int scrollbarX = x + w - 3 - padding;

            // Track
            graphics.fill(scrollbarX, contentY, scrollbarX + 3, contentY + contentH, 0xFF000000);
            // Thumb
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + 3, scrollbarY + scrollbarHeight, scrollbarColor);
        }
    }

    private boolean isChildInView(AbstractWidget child, int contentTop, int contentHeight) {
        return child.getY() + child.getHeight() > contentTop && child.getY() < contentTop + contentHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.clicked(mouseX, mouseY)) return false;

        // Unfocus all children before handling new click
        for (AbstractWidget child : children) {
            child.setFocused(false);
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            AbstractWidget child = children.get(i);
            if (child.isMouseOver(mouseX, mouseY)) {
                if (child.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.clicked(mouseX, mouseY)) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            AbstractWidget child = children.get(i);
            if (child.isMouseOver(mouseX, mouseY)) {
                if (child.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.clicked(mouseX, mouseY)) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            AbstractWidget child = children.get(i);
            if (child.isMouseOver(mouseX, mouseY)) {
                if (child.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!this.clicked(mouseX, mouseY)) return false;

        // Offer scroll to children first
        for (int i = children.size() - 1; i >= 0; i--) {
            AbstractWidget child = children.get(i);
            if (child.isMouseOver(mouseX, mouseY)) {
                if (child.mouseScrolled(mouseX, mouseY, amount)) {
                    return true;
                }
            }
        }

        // Scroll the panel
        setScrollOffset(scrollOffset - (int) (amount * 12));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return false;
    }

    public void tick() {
        for (AbstractWidget child : children) {
            if (child instanceof NumberPickerWidget picker) {
                picker.tick();
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
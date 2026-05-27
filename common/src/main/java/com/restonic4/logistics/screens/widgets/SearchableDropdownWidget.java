package com.restonic4.logistics.screens.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SearchableDropdownWidget<T> extends AbstractWidget {
    private final List<DropdownEntry<T>> allOptions;
    private List<DropdownEntry<T>> filteredOptions;
    private DropdownEntry<T> selected;
    private final Consumer<T> onSelect;

    private boolean expanded = false;
    private final EditBox searchBar;
    private double scrollAmount = 0;
    private SelectionRenderer<T> selectionRenderer;

    // Configuration constants
    private final int itemHeight = 14;
    private final int menuWidth = 120;
    private final int iconSize = 10;
    private final int iconPadding = 4;

    public SearchableDropdownWidget(int x, int y, int width, int height, Component message, List<DropdownEntry<T>> options, Consumer<T> onSelect) {
        super(x, y, width, height, message);
        this.allOptions = new ArrayList<>(options);
        this.filteredOptions = new ArrayList<>(options);
        this.onSelect = onSelect;
        this.selected = options.isEmpty() ? null : options.get(0);
        this.selectionRenderer = new DefaultSelectionRenderer<>();

        Font font = Minecraft.getInstance().font;
        this.searchBar = new EditBox(font, x, y, width, height, Component.literal("Search..."));
        this.searchBar.setResponder(this::onSearch);
        this.searchBar.setVisible(false);
        this.searchBar.setFocused(false);
        this.searchBar.setTextColor(0xFFFFFFFF);
        this.searchBar.setBordered(false);
    }

    /**
     * Flexible icon system supporting flat textures, 3D blocks, and item models.
     */
    @FunctionalInterface
    public interface DropdownIcon {
        void render(GuiGraphics graphics, int x, int y, int size);

        static DropdownIcon of(ResourceLocation texture) {
            if (texture == null) return null;
            return (graphics, x, y, size) -> graphics.blit(texture, x, y, 0, 0, size, size, size, size);
        }

        static DropdownIcon of(ItemStack itemStack) {
            if (itemStack == null || itemStack.isEmpty()) return null;
            return (graphics, x, y, size) -> {
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 0);
                if (size != 16) {
                    float scale = (float) size / 16.0f;
                    graphics.pose().scale(scale, scale, 1.0f);
                }
                graphics.renderFakeItem(itemStack, 0, 0);
                graphics.pose().popPose();
            };
        }

        static DropdownIcon of(Item item) {
            return of(new ItemStack(item));
        }

        static DropdownIcon of(Block block) {
            return of(new ItemStack(block));
        }
    }

    @FunctionalInterface
    public interface SelectionRenderer<T> {
        void render(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                    DropdownEntry<T> selected, boolean isHovered);
    }

    public static class DefaultSelectionRenderer<T> implements SelectionRenderer<T> {
        private final int iconSize;
        private final int iconPadding;
        private final int textOffsetX;

        public DefaultSelectionRenderer() {
            this(10, 4, 5);
        }

        public DefaultSelectionRenderer(int iconSize, int iconPadding, int textOffsetX) {
            this.iconSize = iconSize;
            this.iconPadding = iconPadding;
            this.textOffsetX = textOffsetX;
        }

        @Override
        public void render(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                           DropdownEntry<T> selected, boolean isHovered) {
            int currentX = x + textOffsetX;
            if (selected != null) {
                if (selected.icon() != null) {
                    selected.icon().render(graphics, currentX, y + (height - iconSize) / 2, iconSize);
                    currentX += iconSize + iconPadding;
                }
                graphics.drawString(font, selected.label(), currentX, y + (height - 8) / 2, 0xFFFFFFFF);
            } else {
                graphics.drawString(font, "Select...", currentX, y + (height - 8) / 2, 0xFF777777);
            }
        }
    }

    public static class CompactItemSelectorRenderer<T> implements SelectionRenderer<T> {
        private final int padding;
        private final float textScale;

        public CompactItemSelectorRenderer(int padding, float textScale) {
            this.padding = padding;
            this.textScale = textScale;
        }

        @Override
        public void render(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                           DropdownEntry<T> selected, boolean isHovered) {
            if (selected == null) {
                String empty = "?";
                graphics.drawString(font, empty, x + (width - font.width(empty)) / 2, y + (height - 8) / 2, 0xFF777777);
                return;
            }

            if (selected.icon() != null) {
                int maxSize = Math.min(width, height) - (padding * 2);
                int iconX = x + (width - maxSize) / 2;
                int iconY = y + (height - maxSize) / 2;
                selected.icon().render(graphics, iconX, iconY, maxSize);
            }

            if (textScale > 0) {
                Component text = selected.label();
                int textWidth = font.width(text);

                graphics.pose().pushPose();
                graphics.pose().translate(x + width / 2.0f, y + height - 2, 0);
                graphics.pose().scale(textScale, textScale, 1.0f);
                graphics.drawString(font, text, (int) (-textWidth / 2), -8, 0xFFFFFFFF);
                graphics.pose().popPose();
            }
        }
    }

    public static record DropdownEntry<T>(T value, Component label, DropdownIcon icon) {}

    private void onSearch(String text) {
        this.filteredOptions = allOptions.stream()
                .filter(e -> e.label().getString().toLowerCase().contains(text.toLowerCase()))
                .collect(Collectors.toList());
        this.scrollAmount = 0;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
        graphics.renderOutline(getX(), getY(), width, height, (isHovered || expanded) ? 0xFFFFFFFF : 0xFFA0A0A0);

        if (!expanded) {
            selectionRenderer.render(graphics, mc.font, getX(), getY(), width, height, selected, isHovered);
        } else {
            searchBar.setX(getX() + 4);
            searchBar.setY(getY() + (height - 8) / 2);
            searchBar.render(graphics, mouseX, mouseY, partialTick);
        }

        if (expanded) {
            renderMenu(graphics, mouseX, mouseY);
        }
    }

    void renderMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        int menuX = (getX() + width + menuWidth > screenWidth) ? getX() - menuWidth : getX() + width;
        int maxMenuHeight = (int) (screenHeight * 0.5);
        int totalContentHeight = filteredOptions.size() * itemHeight;
        int actualMenuHeight = Math.min(maxMenuHeight, totalContentHeight);

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 800);

        graphics.fill(menuX, getY(), menuX + menuWidth, getY() + actualMenuHeight, 0xFF101010);
        graphics.renderOutline(menuX, getY(), menuWidth, actualMenuHeight, 0xFFFFFFFF);

        graphics.enableScissor(menuX, getY(), menuX + menuWidth, getY() + actualMenuHeight);

        for (int i = 0; i < filteredOptions.size(); i++) {
            DropdownEntry<T> entry = filteredOptions.get(i);
            int itemY = (int) (getY() + (i * itemHeight) - scrollAmount);

            if (itemY + itemHeight > getY() && itemY < getY() + actualMenuHeight) {
                boolean isItemHovered = mouseX >= menuX && mouseX <= menuX + menuWidth && mouseY >= itemY && mouseY < itemY + itemHeight;

                if (isItemHovered) {
                    graphics.fill(menuX + 1, itemY, menuX + menuWidth - 1, itemY + itemHeight, 0x44FFFFFF);
                }

                if (entry.icon() != null) {
                    entry.icon().render(graphics, menuX + 4, itemY + 2, iconSize);
                    graphics.drawString(mc.font, entry.label(), menuX + 6 + iconSize + iconPadding, itemY + 3, 0xFFFFFFFF, false);
                } else {
                    graphics.drawString(mc.font, entry.label(), menuX + 6, itemY + 3, 0xFFFFFFFF, false);
                }
            }
        }

        graphics.disableScissor();

        if (totalContentHeight > actualMenuHeight) {
            int scrollbarHeight = (int) (((double) actualMenuHeight / totalContentHeight) * actualMenuHeight);
            int scrollbarPos = (int) ((scrollAmount / (totalContentHeight - actualMenuHeight)) * (actualMenuHeight - scrollbarHeight));
            graphics.fill(menuX + menuWidth - 3, getY() + scrollbarPos, menuX + menuWidth - 1, getY() + scrollbarPos + scrollbarHeight, 0xFFAAAAAA);
        }

        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (expanded) {
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int menuX = (getX() + width + menuWidth > screenWidth) ? getX() - menuWidth : getX() + width;
            int actualMenuHeight = Math.min((int)(Minecraft.getInstance().getWindow().getGuiScaledHeight() * 0.5), filteredOptions.size() * itemHeight);

            if (mouseX >= menuX && mouseX <= menuX + menuWidth && mouseY >= getY() && mouseY <= getY() + actualMenuHeight) {
                for (int i = 0; i < filteredOptions.size(); i++) {
                    int itemY = (int) (getY() + (i * itemHeight) - scrollAmount);
                    if (mouseY >= itemY && mouseY < itemY + itemHeight) {
                        selectEntry(filteredOptions.get(i));
                        closeMenu();
                        return true;
                    }
                }
                return true;
            }
        }

        if (this.clicked(mouseX, mouseY)) {
            if (this.expanded) {
                closeMenu();
            } else {
                openMenu();
            }
            return true;
        }

        if (expanded) {
            closeMenu();
        }

        return false;
    }

    private void openMenu() {
        this.expanded = true;
        this.searchBar.setVisible(true);
        this.searchBar.setFocused(true);
        this.searchBar.setValue("");
    }

    public void closeMenu() {
        this.expanded = false;
        this.searchBar.setVisible(false);
        this.searchBar.setFocused(false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (expanded) {
            int totalContentHeight = filteredOptions.size() * itemHeight;
            int maxMenuHeight = (int) (Minecraft.getInstance().getWindow().getGuiScaledHeight() * 0.5);
            int maxScroll = Math.max(0, totalContentHeight - maxMenuHeight);
            this.scrollAmount = Mth.clamp(this.scrollAmount - (amount * 12), 0, maxScroll);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (expanded) {
            if (searchBar.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (keyCode == 256) { // ESC
                closeMenu();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (expanded && searchBar.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.visible) return false;

        if (mouseX >= getX() && mouseX <= getX() + this.width && mouseY >= getY() && mouseY <= getY() + this.height) {
            return true;
        }

        if (expanded) {
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int menuX = (getX() + width + menuWidth > screenWidth) ? getX() - menuWidth : getX() + width;
            int actualMenuHeight = Math.min((int)(Minecraft.getInstance().getWindow().getGuiScaledHeight() * 0.5), filteredOptions.size() * itemHeight);

            if (mouseX >= menuX && mouseX <= menuX + menuWidth && mouseY >= getY() && mouseY <= getY() + actualMenuHeight) {
                return true;
            }
        }

        return false;
    }

    public void setOptions(List<DropdownEntry<T>> newOptions) {
        this.allOptions.clear();
        this.allOptions.addAll(newOptions);

        if (this.selected != null && !this.allOptions.contains(this.selected)) {
            this.selected = this.allOptions.isEmpty() ? null : this.allOptions.get(0);
        }

        onSearch(searchBar.getValue());
    }

    public void setOptions(List<DropdownEntry<T>> newOptions, T selectedValue) {
        setOptions(newOptions);
        setSelectedValue(selectedValue);
    }

    public boolean setSelectedValue(T value) {
        for (DropdownEntry<T> entry : allOptions) {
            if (Objects.equals(entry.value(), value)) {
                selectEntry(entry);
                return true;
            }
        }
        return false;
    }

    public boolean setSelectedEntry(DropdownEntry<T> entry) {
        if (allOptions.contains(entry)) {
            selectEntry(entry);
            return true;
        }
        return false;
    }

    public boolean setSelectedIndex(int index) {
        if (index >= 0 && index < allOptions.size()) {
            selectEntry(allOptions.get(index));
            return true;
        }
        return false;
    }

    public void clearSelection() {
        this.selected = null;
    }

    private void selectEntry(DropdownEntry<T> entry) {
        this.selected = entry;
        if (this.onSelect != null && entry != null) {
            this.onSelect.accept(entry.value());
        }
    }

    public void setSelectionRenderer(SelectionRenderer<T> renderer) {
        this.selectionRenderer = renderer != null ? renderer : new DefaultSelectionRenderer<>();
    }

    public SelectionRenderer<T> getSelectionRenderer() {
        return this.selectionRenderer;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

    public DropdownEntry<T> getSelectedEntry() {
        return this.selected;
    }

    public T getSelectedValue() {
        return this.selected != null ? this.selected.value() : null;
    }

    public List<DropdownEntry<T>> getAllOptions() {
        return List.copyOf(this.allOptions);
    }

    public List<DropdownEntry<T>> getFilteredOptions() {
        return List.copyOf(this.filteredOptions);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void renderMenuOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (expanded) {
            renderMenu(graphics, mouseX, mouseY);
        }
    }

    public void setDropdownWidth(int width) {
        this.setWidth(width);
        this.searchBar.setWidth(Math.max(20, width - 8));
    }

    public void setSelectedValueSilently(T value) {
        for (DropdownEntry<T> entry : allOptions) {
            if (Objects.equals(entry.value(), value)) {
                this.selected = entry;
                return;
            }
        }
    }
}
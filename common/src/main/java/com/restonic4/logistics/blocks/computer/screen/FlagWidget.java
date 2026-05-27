package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.blocks.computer.screen.ProtectionTabDummyData.ActionType;
import com.restonic4.logistics.blocks.computer.screen.ProtectionTabDummyData.FlagState;
import com.restonic4.logistics.blocks.computer.screen.ProtectionTabDummyData.ProtectionFlag;
import com.restonic4.logistics.screens.widgets.NumberPickerWidget;
import com.restonic4.logistics.screens.widgets.SearchableDropdownWidget;
import com.restonic4.logistics.screens.widgets.ToggleWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FlagWidget extends AbstractWidget {
    private final ProtectionFlag flag;
    private FlagState state;
    private final Consumer<FlagState> onChanged;

    private ToggleWidget toggle;
    private SearchableDropdownWidget<ActionType> actionDropdown;
    private NumberPickerWidget damagePicker;
    private EditBox messageField;

    private static final int BG = 0xFF1E1E1E;
    private static final int BORDER = 0xFF2A2A2A;
    private static final int MSG_BOX_HEIGHT = 16;

    public FlagWidget(int x, int y, int width, int height, ProtectionFlag flag, FlagState initialState, Consumer<FlagState> onChanged) {
        super(x, y, width, height, Component.literal(flag.name));
        this.flag = flag;
        this.state = initialState;
        this.onChanged = onChanged;
        buildWidgets();
    }

    private void buildWidgets() {
        int x = getX();
        int y = getY();
        int w = getWidth();

        toggle = new ToggleWidget(x + w - 36, y + 4, 28, 14, state.enabled, enabled -> {
            state.enabled = enabled;
            notifyChanged();
        });

        createActionDropdown(x, y, w);
        rebuildConfigWidget();
    }

    private void createActionDropdown(int x, int y, int w) {
        List<SearchableDropdownWidget.DropdownEntry<ActionType>> actions = new ArrayList<>();
        for (ActionType action : flag.supportedActions) {
            actions.add(new SearchableDropdownWidget.DropdownEntry<>(action, Component.literal(action.name()), null));
        }

        boolean needsConfig = state.action == ActionType.DAMAGE || state.action == ActionType.MESSAGE;
        int dropdownWidth = needsConfig ? (w - 16) / 2 : w - 8;

        actionDropdown = new SearchableDropdownWidget<>(
                x + 4, y + 38, dropdownWidth, MSG_BOX_HEIGHT,
                Component.empty(), actions, action -> {
            state.action = action;
            updateActionDropdownSize();
            rebuildConfigWidget();
            notifyChanged();
        });
        actionDropdown.setSelectedValueSilently(state.action);
    }

    private void updateActionDropdownSize() {
        int w = getWidth();
        boolean needsConfig = state.action == ActionType.DAMAGE || state.action == ActionType.MESSAGE;
        int dropdownWidth = needsConfig ? (w - 16) / 2 : w - 8;
        actionDropdown.setX(getX() + 4);
        actionDropdown.setY(getY() + 38);
        actionDropdown.setDropdownWidth(dropdownWidth);
    }

    private void rebuildConfigWidget() {
        int x = getX();
        int y = getY();
        int w = getWidth();

        damagePicker = null;
        messageField = null;

        if (state.action == ActionType.DAMAGE) {
            int pickerX = x + (w - 8) / 2 + 4;
            int pickerY = y + 38;
            int pickerW = (w - 16) / 2;

            damagePicker = new NumberPickerWidget(pickerX, pickerY, pickerW, MSG_BOX_HEIGHT, Component.empty(), state.damageValue, value -> {
                state.damageValue = value;
                notifyChanged();
            });
            damagePicker.setDecimalPlaces(1);
            damagePicker.setStep(0.5);
            damagePicker.setRange(0, 100);
        } else if (state.action == ActionType.MESSAGE) {
            int fieldX = x + (w - 8) / 2 + 4;
            int fieldY = y + 38;
            int fieldW = (w - 16) / 2;

            Font font = Minecraft.getInstance().font;
            // Position exactly like NumberPickerWidget: inset 4 px left, centred vertically
            int textX = fieldX + 4;
            int textY = fieldY + (MSG_BOX_HEIGHT - 8) / 2; // baseline lands at visual centre
            messageField = new EditBox(font, textX, textY, Math.max(20, fieldW - 8), MSG_BOX_HEIGHT, Component.empty());
            messageField.setValue(state.message);
            messageField.setResponder(text -> {
                state.message = text;
                notifyChanged();
            });
            messageField.setTextColor(0xFFFFFFFF);
            messageField.setBordered(false);
            messageField.setMaxLength(128);
            messageField.setEditable(true);
            messageField.setVisible(true);
            // NOTE: we do NOT use setSuggestion() – we draw the placeholder manually so
            // it never overlaps real text and we control the position exactly.
        }
    }

    private void notifyChanged() {
        if (onChanged != null) onChanged.accept(state);
    }

    /** Locks internal widgets to the FlagWidget's current screen position. */
    private void syncWidgetPositions() {
        int x = getX();
        int y = getY();
        int w = getWidth();

        toggle.setX(x + w - 36);
        toggle.setY(y + 4);

        actionDropdown.setX(x + 4);
        actionDropdown.setY(y + 38);

        if (damagePicker != null) {
            damagePicker.setX(x + (w - 8) / 2 + 4);
            damagePicker.setY(y + 38);
        }

        if (messageField != null) {
            int fieldX = x + (w - 8) / 2 + 4;
            int fieldY = y + 38;
            int fieldW = (w - 16) / 2;
            messageField.setX(fieldX + 4);
            messageField.setY(fieldY + (MSG_BOX_HEIGHT - 8) / 2);
            messageField.setWidth(Math.max(20, fieldW - 8));
        }
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            if (messageField != null && messageField.isFocused()) {
                messageField.setFocused(false);
            }
            if (actionDropdown != null && actionDropdown.isExpanded()) {
                actionDropdown.closeMenu();
            }
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY)) return true;
        if (actionDropdown != null && actionDropdown.isExpanded() && actionDropdown.isMouseOver(mouseX, mouseY)) {
            return true;
        }
        return false;
    }

    public boolean isDropdownExpanded() {
        return actionDropdown != null && actionDropdown.isExpanded();
    }

    public void closeDropdown() {
        if (actionDropdown != null) actionDropdown.closeMenu();
    }

    public void renderDropdownOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (actionDropdown != null && actionDropdown.isExpanded()) {
            actionDropdown.renderMenuOverlay(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        syncWidgetPositions();

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        graphics.fill(x, y, x + w, y + h, BG);
        graphics.renderOutline(x, y, w, h, BORDER);

        Font font = Minecraft.getInstance().font;
        String name = flag.name;
        int nameMaxWidth = w - 44;
        if (font.width(name) > nameMaxWidth) {
            name = font.plainSubstrByWidth(name, nameMaxWidth - font.width("...")) + "...";
        }
        graphics.drawString(font, name, x + 4, y + 6, 0xFFFFFFFF, false);

        toggle.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawString(font, "Action:", x + 4, y + 28, 0xFFAAAAAA, false);

        actionDropdown.render(graphics, mouseX, mouseY, partialTick);

        if (damagePicker != null) {
            damagePicker.render(graphics, mouseX, mouseY, partialTick);
        }

        if (messageField != null) {
            int fieldX = x + (w - 8) / 2 + 4;
            int fieldY = y + 38;
            int fieldW = (w - 16) / 2;

            // Black backing + outline (exactly like NumberPickerWidget's field chrome)
            graphics.fill(fieldX, fieldY, fieldX + fieldW, fieldY + MSG_BOX_HEIGHT, 0xFF000000);

            boolean fieldHovered = mouseX >= fieldX && mouseX < fieldX + fieldW
                    && mouseY >= fieldY && mouseY < fieldY + MSG_BOX_HEIGHT;
            int borderColor = (fieldHovered || messageField.isFocused()) ? 0xFFFFFFFF : 0xFFA0A0A0;
            graphics.renderOutline(fieldX, fieldY, fieldW, MSG_BOX_HEIGHT, borderColor);

            // Render the actual EditBox
            messageField.render(graphics, mouseX, mouseY, partialTick);

            // Manual placeholder – only when empty and not focused.
            // Use the EditBox's own Y position as the baseline (it's already vertically centred).
            if (messageField.getValue().isEmpty() && !messageField.isFocused()) {
                graphics.drawString(font, "Enter message...", messageField.getX(), messageField.getY(), 0xFF777777, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        syncWidgetPositions();

        int fieldX = getX() + (getWidth() - 8) / 2 + 4;
        int fieldY = getY() + 38;
        int fieldW = (getWidth() - 16) / 2;
        boolean onMessageField = messageField != null &&
                mouseX >= fieldX && mouseX < fieldX + fieldW &&
                mouseY >= fieldY && mouseY < fieldY + MSG_BOX_HEIGHT;

        // Unfocus the text field when clicking anywhere else inside this widget
        if (!onMessageField && messageField != null && messageField.isFocused()) {
            messageField.setFocused(false);
        }

        if (toggle.mouseClicked(mouseX, mouseY, button)) return true;
        if (actionDropdown.mouseClicked(mouseX, mouseY, button)) return true;
        if (damagePicker != null && damagePicker.mouseClicked(mouseX, mouseY, button)) return true;

        if (onMessageField) {
            messageField.setFocused(true);
            this.setFocused(true);
            messageField.mouseClicked(mouseX, mouseY, button);
            return true;
        }

        return this.clicked(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        syncWidgetPositions();
        if (actionDropdown.mouseScrolled(mouseX, mouseY, amount)) return true;
        if (damagePicker != null && damagePicker.mouseScrolled(mouseX, mouseY, amount)) return true;
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        syncWidgetPositions();
        if (actionDropdown.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (damagePicker != null && damagePicker.keyPressed(keyCode, scanCode, modifiers)) return true;

        if (messageField != null && messageField.isFocused()) {
            // Enter / Escape commits and blurs so you can move to other widgets
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                messageField.setFocused(false);
                this.setFocused(false);
                return true;
            }
            if (messageField.keyPressed(keyCode, scanCode, modifiers)) return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        syncWidgetPositions();
        if (actionDropdown.charTyped(codePoint, modifiers)) return true;
        if (damagePicker != null && damagePicker.charTyped(codePoint, modifiers)) return true;
        if (messageField != null && messageField.isFocused() && messageField.charTyped(codePoint, modifiers)) return true;
        return false;
    }

    public void tick() {
        if (damagePicker != null) damagePicker.tick();
        if (messageField != null) messageField.tick();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
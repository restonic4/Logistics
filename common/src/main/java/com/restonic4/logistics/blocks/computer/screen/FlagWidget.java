package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.blocks.computer.screen.ProtectionTabDummyData;
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

        // Toggle (top right)
        toggle = new ToggleWidget(x + w - 36, y + 4, 28, 14, state.enabled, enabled -> {
            state.enabled = enabled;
            notifyChanged();
        });

        // Action dropdown
        List<SearchableDropdownWidget.DropdownEntry<ActionType>> actions = new ArrayList<>();
        for (ActionType action : flag.supportedActions) {
            actions.add(new SearchableDropdownWidget.DropdownEntry<>(action, Component.literal(action.name()), null));
        }

        boolean needsConfig = state.action == ActionType.DAMAGE || state.action == ActionType.MESSAGE;
        int dropdownWidth = needsConfig ? (w - 16) / 2 : w - 8;

        actionDropdown = new SearchableDropdownWidget<>(
                x + 4, y + 26, dropdownWidth, 16,
                Component.empty(), actions, action -> {
            state.action = action;
            rebuildConfigWidget();
            notifyChanged();
        });
        actionDropdown.setSelectedValue(state.action);

        rebuildConfigWidget();
    }

    private void rebuildConfigWidget() {
        int x = getX();
        int y = getY();
        int w = getWidth();

        damagePicker = null;
        messageField = null;

        if (state.action == ActionType.DAMAGE) {
            int pickerX = x + (w - 8) / 2 + 4;
            int pickerY = y + 26;
            int pickerW = (w - 16) / 2;

            damagePicker = new NumberPickerWidget(pickerX, pickerY, pickerW, 16, Component.empty(), state.damageValue, value -> {
                state.damageValue = value;
                notifyChanged();
            });
            damagePicker.setDecimalPlaces(1);
            damagePicker.setStep(0.5);
            damagePicker.setRange(0, 100);
        } else if (state.action == ActionType.MESSAGE) {
            int fieldX = x + (w - 8) / 2 + 4;
            int fieldY = y + 28;
            int fieldW = (w - 16) / 2;

            Font font = Minecraft.getInstance().font;
            messageField = new EditBox(font, fieldX, fieldY, fieldW, 12, Component.empty());
            messageField.setValue(state.message);
            messageField.setResponder(text -> {
                state.message = text;
                notifyChanged();
            });
            messageField.setTextColor(0xFFFFFFFF);
            messageField.setBordered(false);
        }
    }

    private void notifyChanged() {
        if (onChanged != null) {
            onChanged.accept(state);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        // Background
        graphics.fill(x, y, x + w, y + h, BG);
        graphics.renderOutline(x, y, w, h, BORDER);

        // Flag name (top left)
        Font font = Minecraft.getInstance().font;
        String name = flag.name;
        int nameMaxWidth = w - 44;
        if (font.width(name) > nameMaxWidth) {
            name = font.plainSubstrByWidth(name, nameMaxWidth - font.width("...")) + "...";
        }
        graphics.drawString(font, name, x + 4, y + 6, 0xFFFFFFFF, false);

        // Toggle
        toggle.setX(x + w - 36);
        toggle.setY(y + 4);
        toggle.render(graphics, mouseX, mouseY, partialTick);

        // Action label
        graphics.drawString(font, "Action:", x + 4, y + 28, 0xFFAAAAAA, false);

        // Action dropdown
        actionDropdown.setX(x + 4);
        actionDropdown.setY(y + 38);
        actionDropdown.render(graphics, mouseX, mouseY, partialTick);

        // Config widget
        if (damagePicker != null) {
            damagePicker.setX(x + (w - 8) / 2 + 4);
            damagePicker.setY(y + 38);
            damagePicker.render(graphics, mouseX, mouseY, partialTick);
        }
        if (messageField != null) {
            messageField.setX(x + (w - 8) / 2 + 4);
            messageField.setY(y + 40);
            messageField.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (toggle.mouseClicked(mouseX, mouseY, button)) return true;
        if (actionDropdown.mouseClicked(mouseX, mouseY, button)) return true;
        if (damagePicker != null && damagePicker.mouseClicked(mouseX, mouseY, button)) return true;
        if (messageField != null && messageField.mouseClicked(mouseX, mouseY, button)) return true;
        return this.clicked(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (actionDropdown.mouseScrolled(mouseX, mouseY, amount)) return true;
        if (damagePicker != null && damagePicker.mouseScrolled(mouseX, mouseY, amount)) return true;
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (actionDropdown.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (damagePicker != null && damagePicker.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (messageField != null && messageField.keyPressed(keyCode, scanCode, modifiers)) return true;
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (actionDropdown.charTyped(codePoint, modifiers)) return true;
        if (damagePicker != null && damagePicker.charTyped(codePoint, modifiers)) return true;
        if (messageField != null && messageField.charTyped(codePoint, modifiers)) return true;
        return false;
    }

    public void tick() {
        if (damagePicker != null) damagePicker.tick();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
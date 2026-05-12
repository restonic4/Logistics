package com.restonic4.logistics.screens.widgets;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * A scalable number input widget with optional arrows, Ctrl-scroll stepping,
 * configurable precision, min/max limits, and independent behaviour locks.
 */
public class NumberPickerWidget extends AbstractWidget {
    private final EditBox textField;
    private final Consumer<Double> onValueChanged;

    private double value;
    private double minValue = Double.NEGATIVE_INFINITY;
    private double maxValue = Double.POSITIVE_INFINITY;
    private double step = 1.0;
    private int decimalPlaces = 0;

    private boolean arrowsEnabled = true;
    private boolean scrollEnabled = true;
    private boolean textInputEnabled = true;

    private boolean internalUpdate = false;
    private boolean upArrowHovered = false;
    private boolean downArrowHovered = false;

    // Visual constants
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_BG = 0xFF000000;
    private static final int COLOR_BORDER = 0xFFA0A0A0;
    private static final int COLOR_BORDER_FOCUSED = 0xFFFFFFFF;
    private static final int COLOR_ARROW = 0xFFFFFFFF;
    private static final int COLOR_ARROW_HOVER = 0xFFFFFF55;
    private static final int COLOR_ARROW_DISABLED = 0xFF555555;
    private static final int COLOR_DIVIDER = 0xFF666666;

    // Arrow layout
    private static final int ARROW_COLUMN_MIN_WIDTH = 14;
    /** Uniform inset from every edge of the arrow column and between the two arrows. */
    private static final int ARROW_INSET = 3;

    public NumberPickerWidget(int x, int y, int width, int height, Component message,
                              double initialValue, Consumer<Double> onValueChanged) {
        super(x, y, width, height, message);
        this.value = initialValue;
        this.onValueChanged = onValueChanged;

        Font font = Minecraft.getInstance().font;
        int arrowW = getArrowColumnWidth();
        this.textField = new EditBox(font, x + 4, y + (height - 8) / 2,
                width - arrowW - 8, height, Component.empty());
        this.textField.setResponder(this::onTextChanged);
        this.textField.setTextColor(COLOR_TEXT);
        this.textField.setBordered(false);
        this.textField.setEditable(true);
        this.textField.setVisible(true);
        this.textField.setValue(formatValue(initialValue));
    }

    /* ======================== Configuration ======================== */

    /** Sets the allowed range. Values are clamped and rounded automatically. */
    public void setRange(double min, double max) {
        this.minValue = min;
        this.maxValue = max;
        setValue(this.value); // reclamp
    }

    /** 0 = integers only. >0 = enforce that many decimal places. */
    public void setDecimalPlaces(int places) {
        this.decimalPlaces = Math.max(0, places);
        setValue(this.value); // reformat
    }

    /** How much one arrow click or one scroll step changes the value. */
    public void setStep(double step) {
        this.step = step > 0 ? step : 1.0;
    }

    /** Show/hide the integrated arrow buttons. */
    public void setArrowsEnabled(boolean enabled) {
        this.arrowsEnabled = enabled;
    }

    /** Allow/disallow Ctrl+mouse wheel stepping. */
    public void setScrollEnabled(boolean enabled) {
        this.scrollEnabled = enabled;
    }

    /** Allow/disallow manual keyboard typing. When false the field is read-only. */
    public void setTextInputEnabled(boolean enabled) {
        this.textInputEnabled = enabled;
        this.textField.setEditable(enabled);
        if (!enabled && this.textField.isFocused()) {
            this.textField.setFocused(false);
        }
    }

    /* ======================== Value API ======================== */

    public double getValue() {
        return value;
    }

    public void setValue(double newValue) {
        double clamped = clampAndRound(newValue);
        if (clamped == this.value) {
            internalUpdate = true;
            this.textField.setValue(formatValue(this.value));
            internalUpdate = false;
            return;
        }

        this.value = clamped;
        internalUpdate = true;
        this.textField.setValue(formatValue(this.value));
        internalUpdate = false;

        if (onValueChanged != null) {
            onValueChanged.accept(this.value);
        }
    }

    private double clampAndRound(double val) {
        val = Mth.clamp(val, minValue, maxValue);
        if (decimalPlaces == 0) {
            return Math.round(val);
        }
        double factor = Math.pow(10, decimalPlaces);
        return Math.round(val * factor) / factor;
    }

    private String formatValue(double val) {
        if (decimalPlaces == 0) {
            return String.format(Locale.US, "%.0f", val);
        }
        return String.format(Locale.US, "%." + decimalPlaces + "f", val);
    }

    /* ======================== Text Input Handling ======================== */

    private void onTextChanged(String text) {
        if (internalUpdate) return;

        if (text.isEmpty() || text.equals("-") || text.equals(".") || text.equals("-.")) {
            return;
        }

        if (!isValidNumberString(text)) {
            revertText();
            return;
        }

        try {
            double parsed = Double.parseDouble(text);
            double clamped = clampAndRound(parsed);
            if (clamped != this.value) {
                this.value = clamped;
                if (onValueChanged != null) {
                    onValueChanged.accept(this.value);
                }
            }
        } catch (NumberFormatException e) {
            revertText();
        }
    }

    private void revertText() {
        internalUpdate = true;
        this.textField.setValue(formatValue(this.value));
        internalUpdate = false;
    }

    private boolean isValidNumberString(String text) {
        if (text.isEmpty() || text.equals("-") || text.equals(".") || text.equals("-.")) {
            return true;
        }
        String regex = decimalPlaces == 0
                ? "-?\\d+"
                : "-?\\d*\\.?\\d{0," + decimalPlaces + "}";
        return text.matches(regex);
    }

    /* ======================== Layout ======================== */

    private int getArrowColumnWidth() {
        if (!arrowsEnabled) return 0;
        return Math.max(ARROW_COLUMN_MIN_WIDTH, this.height - 6);
    }

    /* ======================== Rendering ======================== */

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int arrowColumnWidth = getArrowColumnWidth();
        int textW = this.width - arrowColumnWidth - 8;

        this.textField.setX(this.getX() + 4);
        this.textField.setY(this.getY() + (this.height - 8) / 2);
        this.textField.setWidth(textW);
        this.textField.setVisible(this.visible);
        this.textField.setEditable(this.active && this.textInputEnabled);

        // Background & border
        graphics.fill(getX(), getY(), getX() + width, getY() + height, COLOR_BG);
        int border = (this.isHovered || this.textField.isFocused()) ? COLOR_BORDER_FOCUSED : COLOR_BORDER;
        graphics.renderOutline(getX(), getY(), width, height, border);

        // Text field
        this.textField.render(graphics, mouseX, mouseY, partialTick);

        // Arrows
        if (arrowsEnabled) {
            int arrowX = getX() + width - arrowColumnWidth;
            int halfH = height / 2;

            // Divider line
            int dividerX = arrowX + ARROW_INSET;
            graphics.fill(dividerX - 1, getY() + ARROW_INSET, dividerX, getY() + height - ARROW_INSET, COLOR_DIVIDER);

            // Hit areas (full half for usability)
            upArrowHovered = this.active && mouseX >= arrowX && mouseX < getX() + width
                    && mouseY >= getY() && mouseY < getY() + halfH;

            downArrowHovered = this.active && mouseX >= arrowX && mouseX < getX() + width
                    && mouseY >= getY() + halfH && mouseY < getY() + height;

            boolean canUp = this.value < this.maxValue;
            boolean canDown = this.value > this.minValue;

            // Compute perfectly centered arrow bounds
            int arrowW = arrowColumnWidth - ARROW_INSET * 2;
            int arrowH = halfH - ARROW_INSET * 2;

            // Up arrow
            renderArrow(graphics,
                    arrowX + ARROW_INSET,
                    getY() + ARROW_INSET,
                    arrowW,
                    arrowH,
                    true, upArrowHovered, canUp);

            // Down arrow
            renderArrow(graphics,
                    arrowX + ARROW_INSET,
                    getY() + halfH + ARROW_INSET,
                    arrowW,
                    arrowH,
                    false, downArrowHovered, canDown);
        }
    }

    private void renderArrow(GuiGraphics graphics, int x, int y, int w, int h,
                             boolean up, boolean hovered, boolean enabled) {
        int color = enabled ? (hovered ? COLOR_ARROW_HOVER : COLOR_ARROW) : COLOR_ARROW_DISABLED;

        int cx = x + w / 2;
        int cy = y + h / 2;
        int marginX = w / 4;
        int marginY = h / 4;
        int size = Math.min(w - marginX * 2, h - marginY * 2);
        size = Math.max(3, size);

        int half = size / 2;
        int startY = cy - half;

        for (int i = 0; i < size; i++) {
            int halfWidth = up ? i : (size - 1 - i);
            int rowY = startY + i;
            graphics.fill(cx - halfWidth, rowY, cx + halfWidth + 1, rowY + 1, color);
        }
    }

    /* ======================== Interaction ======================== */

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;

        if (arrowsEnabled) {
            int arrowColumnWidth = getArrowColumnWidth();
            int arrowX = getX() + width - arrowColumnWidth;
            int halfH = height / 2;

            if (mouseX >= arrowX && mouseX < getX() + width) {
                if (mouseY >= getY() && mouseY < getY() + halfH) {
                    if (this.value < this.maxValue) {
                        setValue(this.value + step);
                    }
                    return true;
                } else if (mouseY >= getY() + halfH && mouseY < getY() + height) {
                    if (this.value > this.minValue) {
                        setValue(this.value - step);
                    }
                    return true;
                }
            }
        }

        if (this.textField.mouseClicked(mouseX, mouseY, button)) {
            this.textField.setFocused(true);
            this.setFocused(true);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!this.active || !this.visible || !scrollEnabled) return false;

        boolean ctrl = InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);

        if (ctrl && (this.isHovered || this.textField.isFocused())) {
            setValue(this.value + (amount > 0 ? step : -step));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.textField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                this.textField.setFocused(false);
                this.setFocused(false);
                setValue(this.value);
                return true;
            }
            if (this.textInputEnabled) {
                return this.textField.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.textField.isFocused() && this.textInputEnabled) {
            return this.textField.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) {
            if (this.textInputEnabled) {
                this.textField.setFocused(true);
            }
        } else {
            if (this.textField.isFocused()) {
                this.textField.setFocused(false);
                setValue(this.value);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.textField.updateNarration(output);
    }

    /** Call this from your screen's tick() so the cursor blinks properly. */
    public void tick() {
        this.textField.tick();
    }

    public EditBox getTextField() {
        return this.textField;
    }
}
package com.restonic4.logistics.screens;

import com.restonic4.logistics.blocks.base.RenameNodePacket;
import com.restonic4.logistics.networking.ClientNetworking;
import com.restonic4.logistics.screens.widgets.StyledButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class RenameNodeScreen extends Screen {
    private final BlockPos targetBlock;
    private final String currentName;

    private EditBox nameField;
    private StyledButton applyButton;
    private StyledButton cancelButton;

    // Panel geometry — wide and short
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 72;

    // Colors matching the existing UI style
    private static final int COLOR_PANEL_BG = 0xFF121212;
    private static final int COLOR_CONTENT_BG = 0xFF1A1A1A;
    private static final int COLOR_BORDER = 0xFF2A2A2A;

    private static final int COLOR_BUTTON_TEXT = 0xFFFFFFFF;
    private static final int COLOR_BUTTON_BORDER = 0xFF2A2A2A;

    private static final int COLOR_BUTTON_APPLY_BG = 0xFF1A3A1A;
    private static final int COLOR_BUTTON_APPLY_HOVER = 0xFF2A5A2A;
    private static final int COLOR_BUTTON_APPLY_PRESS = 0xFF356A35;

    private static final int COLOR_BUTTON_CANCEL_BG = 0xFF3A1A1A;
    private static final int COLOR_BUTTON_CANCEL_HOVER = 0xFF5A2A2A;
    private static final int COLOR_BUTTON_CANCEL_PRESS = 0xFF6A3535;

    // Padding
    private static final int PAD_X = 12;
    private static final int PAD_Y = 10;
    private static final int BUTTON_GAP = 8;
    private static final int INPUT_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;

    private RenameNodeScreen(BlockPos targetBlock, String currentName) {
        super(Component.literal("Rename Node"));
        this.targetBlock = targetBlock;
        this.currentName = currentName != null ? currentName : "";
    }

    public static void open(BlockPos targetBlock, String currentName) {
        Minecraft.getInstance().setScreen(new RenameNodeScreen(targetBlock, currentName));
    }

    @Override
    protected void init() {
        int panelLeft = (this.width - PANEL_WIDTH) / 2;
        int panelTop = (this.height - PANEL_HEIGHT) / 2;
        int panelRight = panelLeft + PANEL_WIDTH;
        int panelBottom = panelTop + PANEL_HEIGHT;

        int contentLeft = panelLeft + PAD_X;
        int contentRight = panelRight - PAD_X;
        int contentWidth = contentRight - contentLeft;

        int inputY = panelTop + PAD_Y;

        // Name input field — spans most of the width
        int buttonWidth = (contentWidth - BUTTON_GAP) / 2;
        int inputWidth = contentWidth;

        this.nameField = new EditBox(
                this.font,
                contentLeft,
                inputY,
                inputWidth,
                INPUT_HEIGHT,
                Component.literal("Name")
        );
        this.nameField.setValue(this.currentName);
        this.nameField.setTextColor(0xFFFFFFFF);
        this.nameField.setBordered(true);
        this.nameField.setMaxLength(64);
        this.nameField.setFocused(true);
        this.addWidget(this.nameField);
        this.setInitialFocus(this.nameField);

        // Buttons row
        int buttonY = inputY + INPUT_HEIGHT + PAD_Y;

        this.applyButton = new StyledButton(
                contentLeft,
                buttonY,
                buttonWidth,
                BUTTON_HEIGHT,
                Component.literal("Apply"),
                this::onApply
        );
        this.applyButton.withColors(COLOR_BUTTON_APPLY_BG, COLOR_BUTTON_BORDER, COLOR_BUTTON_TEXT)
                .withHoverColor(COLOR_BUTTON_APPLY_HOVER)
                .withPressColor(COLOR_BUTTON_APPLY_PRESS);
        this.addRenderableWidget(this.applyButton);

        this.cancelButton = new StyledButton(
                contentLeft + buttonWidth + BUTTON_GAP,
                buttonY,
                buttonWidth,
                BUTTON_HEIGHT,
                Component.literal("Cancel"),
                this::onCancel
        );
        this.cancelButton.withColors(COLOR_BUTTON_CANCEL_BG, COLOR_BUTTON_BORDER, COLOR_BUTTON_TEXT)
                .withHoverColor(COLOR_BUTTON_CANCEL_HOVER)
                .withPressColor(COLOR_BUTTON_CANCEL_PRESS);
        this.addRenderableWidget(this.cancelButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);

        int panelLeft = (this.width - PANEL_WIDTH) / 2;
        int panelTop = (this.height - PANEL_HEIGHT) / 2;
        int panelRight = panelLeft + PANEL_WIDTH;
        int panelBottom = panelTop + PANEL_HEIGHT;

        // Panel background
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, COLOR_PANEL_BG);

        // Content area background (slightly lighter)
        int contentLeft = panelLeft + 1;
        int contentTop = panelTop + 1;
        int contentRight = panelRight - 1;
        int contentBottom = panelBottom - 1;
        graphics.fill(contentLeft, contentTop, contentRight, contentBottom, COLOR_CONTENT_BG);

        // Panel border
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, COLOR_BORDER);
        graphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, COLOR_BORDER);
        graphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, COLOR_BORDER);
        graphics.fill(panelRight - 1, panelTop, panelRight, panelBottom, COLOR_BORDER);

        // Render the text field (handles its own border, background, and text)
        this.nameField.render(graphics, mouseX, mouseY, delta);

        // Render buttons
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            onApply();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onCancel();
            return true;
        }
        if (this.nameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.nameField.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.nameField.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.nameField);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        onCancel();
    }

    private void onApply() {
        String newName = this.nameField.getValue().trim();
        ClientNetworking.sendToServer(new RenameNodePacket(this.targetBlock, newName));
        this.minecraft.setScreen(null);
    }

    private void onCancel() {
        this.minecraft.setScreen(null);
    }
}
package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.screens.widgets.StyledButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class UnsavedChangesPopup {
    private final Screen parent;
    private final Runnable onSaveAndClose;
    private final Runnable onDiscardAndClose;
    private final Runnable onCancel;

    private StyledButton saveButton;
    private StyledButton discardButton;
    private StyledButton cancelButton;
    private boolean active = false;

    public UnsavedChangesPopup(Screen parent, Runnable onSaveAndClose, Runnable onDiscardAndClose, Runnable onCancel) {
        this.parent = parent;
        this.onSaveAndClose = onSaveAndClose;
        this.onDiscardAndClose = onDiscardAndClose;
        this.onCancel = onCancel;

        this.saveButton = new StyledButton(0, 0, 90, 20, Component.translatable("screen.logistics.generic.save_close"), () -> {
            active = false;
            onSaveAndClose.run();
        });
        this.saveButton.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF);

        this.discardButton = new StyledButton(0, 0, 110, 20, Component.translatable("screen.logistics.generic.discard_close"), () -> {
            active = false;
            onDiscardAndClose.run();
        });
        this.discardButton.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF);

        this.cancelButton = new StyledButton(0, 0, 70, 20, Component.translatable("screen.logistics.generic.cancel"), () -> {
            active = false;
            onCancel.run();
        });
        this.cancelButton.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF);
    }

    public void show() {
        this.active = true;
    }

    public void close() {
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!active) return;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1000);

        int screenW = parent.width;
        int screenH = parent.height;

        // Darken background
        graphics.fill(0, 0, screenW, screenH, 0x88000000);

        // Panel
        int width = 300;
        int height = 100;
        int x = screenW / 2 - width / 2;
        int y = screenH / 2 - height / 2;

        graphics.fill(x, y, x + width, y + height, 0xFF1A1A1A);
        graphics.renderOutline(x, y, width, height, 0xFF2A2A2A);

        // Title
        Font font = Minecraft.getInstance().font;
        String title = Component.translatable("screen.logistics.computer.tab.protector.unsaved").getString();
        graphics.drawString(font, title, x + (width - font.width(title)) / 2, y + 15, 0xFFFFFFFF, false);

        // Buttons
        int btnY = y + 55;
        saveButton.setX(x + 10);
        saveButton.setY(btnY);
        saveButton.render(graphics, mouseX, mouseY, partialTick);

        discardButton.setX(x + 105);
        discardButton.setY(btnY);
        discardButton.render(graphics, mouseX, mouseY, partialTick);

        cancelButton.setX(x + 220);
        cancelButton.setY(btnY);
        cancelButton.render(graphics, mouseX, mouseY, partialTick);

        graphics.pose().popPose();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active) return false;

        if (saveButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (discardButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (cancelButton.mouseClicked(mouseX, mouseY, button)) return true;

        // Click outside = cancel
        int width = 300;
        int height = 100;
        int x = parent.width / 2 - width / 2;
        int y = parent.height / 2 - height / 2;
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            active = false;
            onCancel.run();
            return true;
        }

        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            active = false;
            onCancel.run();
            return true;
        }
        return false;
    }
}
package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.screens.widgets.StyledButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ConfirmDeletePopup {
    private final Screen parent;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    private StyledButton confirmButton;
    private StyledButton cancelButton;
    private boolean active = false;

    public ConfirmDeletePopup(Screen parent, Runnable onConfirm, Runnable onCancel) {
        this.parent = parent;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        this.confirmButton = new StyledButton(0, 0, 80, 20, Component.literal("Delete"), () -> {
            active = false;
            onConfirm.run();
        });
        this.confirmButton.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF);

        this.cancelButton = new StyledButton(0, 0, 80, 20, Component.literal("Cancel"), () -> {
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
        int width = 260;
        int height = 100;
        int x = screenW / 2 - width / 2;
        int y = screenH / 2 - height / 2;

        graphics.fill(x, y, x + width, y + height, 0xFF1A1A1A);
        graphics.renderOutline(x, y, width, height, 0xFF2A2A2A);

        // Title
        Font font = Minecraft.getInstance().font;
        String title = "Delete Role";
        graphics.drawString(font, title, x + (width - font.width(title)) / 2, y + 8, 0xFFFFFFFF, false);

        // Message
        String msg = "Are you sure you want to delete this role?";
        graphics.drawString(font, msg, x + (width - font.width(msg)) / 2, y + 32, 0xFFCCCCCC, false);

        // Buttons
        int btnY = y + 65;
        confirmButton.setX(x + 45);
        confirmButton.setY(btnY);
        confirmButton.render(graphics, mouseX, mouseY, partialTick);

        cancelButton.setX(x + 135);
        cancelButton.setY(btnY);
        cancelButton.render(graphics, mouseX, mouseY, partialTick);

        graphics.pose().popPose();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active) return false;

        if (confirmButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (cancelButton.mouseClicked(mouseX, mouseY, button)) return true;

        // Click outside = cancel
        int width = 260;
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
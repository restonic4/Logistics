package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.screens.widgets.StyledButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class AddRolePopup {
    private final Screen parent;
    private final Consumer<String> onCreate;
    private final Runnable onCancel;

    private EditBox nameField;
    private StyledButton createButton;
    private StyledButton cancelButton;
    private boolean active = false;

    public AddRolePopup(Screen parent, Consumer<String> onCreate, Runnable onCancel) {
        this.parent = parent;
        this.onCreate = onCreate;
        this.onCancel = onCancel;

        Font font = Minecraft.getInstance().font;
        this.nameField = new EditBox(font, 0, 0, 160, 18, Component.literal("Role Name"));
        this.nameField.setTextColor(0xFFFFFFFF);
        this.nameField.setBordered(true);
        this.nameField.setMaxLength(64);

        this.createButton = new StyledButton(0, 0, 80, 20, Component.literal("Create"), () -> {
            if (!nameField.getValue().isBlank()) {
                active = false;
                onCreate.accept(nameField.getValue().trim());
            }
        });
        this.createButton.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF);

        this.cancelButton = new StyledButton(0, 0, 80, 20, Component.literal("Cancel"), () -> {
            active = false;
            onCancel.run();
        });
        this.cancelButton.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF);
    }

    public void show() {
        this.active = true;
        this.nameField.setValue("");
        this.nameField.setFocused(true);
    }

    public void close() {
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public void tick() {
        if (active) {
            nameField.tick();
        }
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
        int width = 200;
        int height = 100;
        int x = screenW / 2 - width / 2;
        int y = screenH / 2 - height / 2;

        graphics.fill(x, y, x + width, y + height, 0xFF1A1A1A);
        graphics.renderOutline(x, y, width, height, 0xFF2A2A2A);

        // Title
        Font font = Minecraft.getInstance().font;
        String title = "Create New Role";
        graphics.drawString(font, title, x + (width - font.width(title)) / 2, y + 8, 0xFFFFFFFF, false);

        // Name field
        nameField.setX(x + 20);
        nameField.setY(y + 32);
        nameField.setWidth(width - 40);
        nameField.render(graphics, mouseX, mouseY, partialTick);

        // Buttons
        createButton.setX(x + 15);
        createButton.setY(y + 65);
        createButton.active = !nameField.getValue().isBlank();
        createButton.render(graphics, mouseX, mouseY, partialTick);

        cancelButton.setX(x + 105);
        cancelButton.setY(y + 65);
        cancelButton.render(graphics, mouseX, mouseY, partialTick);

        graphics.pose().popPose();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active) return false;

        int width = 200;
        int height = 100;
        int x = parent.width / 2 - width / 2;
        int y = parent.height / 2 - height / 2;

        if (nameField.mouseClicked(mouseX, mouseY, button)) return true;
        if (createButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (cancelButton.mouseClicked(mouseX, mouseY, button)) return true;

        // Click outside = cancel
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            active = false;
            onCancel.run();
            return true;
        }

        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active) return false;

        if (keyCode == GLFW.GLFW_KEY_ENTER && !nameField.getValue().isBlank()) {
            active = false;
            onCreate.accept(nameField.getValue().trim());
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            active = false;
            onCancel.run();
            return true;
        }

        return nameField.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!active) return false;
        return nameField.charTyped(codePoint, modifiers);
    }
}
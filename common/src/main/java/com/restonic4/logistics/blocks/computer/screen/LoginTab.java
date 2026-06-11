package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.blocks.computer.ComputerClientLogPushPacket;
import com.restonic4.logistics.blocks.computer.ComputerLogEntry;
import com.restonic4.logistics.networking.ClientNetworking;
import com.restonic4.logistics.screens.tabs.Tab;
import com.restonic4.logistics.screens.widgets.StyledButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public class LoginTab extends Tab {
    private EditBox passwordField;
    private boolean showDeniedMsg = false;

    public LoginTab() {
        super(Component.translatable("screen.logistics.computer.tab.login.title"));
    }

    @Override
    public void init(Screen parent, int x, int y, int width, int height) {
        Font font = Minecraft.getInstance().font;
        this.passwordField = null;
        this.showDeniedMsg = false;

        this.passwordField = new EditBox(font, x + 15, y + 52, 150, 20, Component.translatable("screen.logistics.generic.password"));
        this.passwordField.setMaxLength(32);
        // Mask the password
        this.passwordField.setFormatter((text, bg) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
        parent.addRenderableWidget(this.passwordField);

        StyledButton loginBtn = new StyledButton(x + 15, y + 80, 80, 20, Component.translatable("screen.logistics.computer.tab.login.title"), () -> {
            if (parent instanceof ComputerScreen screen) {
                if (this.passwordField.getValue().equals(ComputerScreen.getComputerNode().getRootPassword())) {
                    screen.performLogin();
                    ClientNetworking.sendToServer(new ComputerClientLogPushPacket(ComputerScreen.getComputerNode().getBlockPos(),
                            ComputerLogEntry.Severity.INFO,
                            Component.translatable("screen.logistics.computer.tab.login.granted").getString()
                    ));
                } else {
                    this.showDeniedMsg = true;
                    ClientNetworking.sendToServer(new ComputerClientLogPushPacket(ComputerScreen.getComputerNode().getBlockPos(),
                            ComputerLogEntry.Severity.ERROR,
                            Component.translatable("screen.logistics.computer.tab.login.denied").getString()
                    ));
                }
            }
        });
        parent.addRenderableWidget(loginBtn);
    }

    @Override
    public void tick() {
        if (this.passwordField != null) this.passwordField.tick();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta, int x, int y, int width, int height) {
        Font font = Minecraft.getInstance().font;
        int currentX = x + 15;
        int currentY = y + 15;

        // Header
        gfx.pose().pushPose();
        gfx.pose().translate(currentX, currentY, 0);
        gfx.pose().scale(1.2f, 1.2f, 1.0f);
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.login.required").getString(), 0, 0, 0xFFFFFFFF, true);
        gfx.pose().popPose();

        currentY += 16;
        gfx.fill(currentX, currentY, x + width - 15, currentY + 1, 0x44FFFFFF);
        currentY += 8;

        // Dynamic strings
        gfx.drawString(font, Component.translatable("screen.logistics.generic.system").getString() + ": " + ComputerScreen.getComputerNode().getSystemName(), currentX, currentY, 0xFF55FF55, false);
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.login.password").getString(), currentX, currentY + 18, 0xFFAAAAAA, false);

        // Error message
        if (this.showDeniedMsg) {
            gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.login.denied").getString(), currentX + 90, currentY + 46, 0xFFFF5555, false);
        }
    }
}
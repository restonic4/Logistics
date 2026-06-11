package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.computer.ComputerInstallPacket;
import com.restonic4.logistics.blocks.computer.OpenComputerPacket;
import com.restonic4.logistics.networking.ClientNetworking;
import com.restonic4.logistics.screens.tabs.Tab;
import com.restonic4.logistics.screens.tabs.TabbedScreen;
import com.restonic4.logistics.screens.widgets.ProgressBarWidget;
import com.restonic4.logistics.screens.widgets.StyledButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class InstallTab extends Tab {
    public enum Step {
        WELCOME,
        ROOT_PASSWORD,
        SYSTEM_NAME,
        SUMMARY,
        ANIMATION
    }

    private Step currentStep = Step.WELCOME;

    // Persistent form state data cache across steps
    private String rootPassword = "";
    private String confirmPassword = "";
    private String systemName = "DragonOS";

    // Tracking widget references for blinking loops
    private EditBox passwordField;
    private EditBox confirmField;
    private EditBox hostnameField;

    private int animationTicks = 0;
    private float animationProgress = 0.0f;
    private String currentSplashText = "";
    private int nextSplashUpdateTick = 0;
    private int pauseTicks = 0;

    public InstallTab() {
        super(Component.translatable("screen.logistics.computer.tab.install.title"));
    }

    @Override
    public void init(Screen parent, int x, int y, int width, int height) {
        Font font = Minecraft.getInstance().font;

        // Reset text boxes tracking references on layout regeneration
        this.passwordField = null;
        this.confirmField = null;
        this.hostnameField = null;

        // Step Tracker Progress Bar - Pinned at the bottom margin area
        int progressBarY = y + height - 20;
        ProgressBarWidget progressBar = new ProgressBarWidget(x + 15, progressBarY, width - 110, 10, currentStep.ordinal() + 1);
        progressBar.setRange(1, Step.values().length);
        progressBar.setBarColor(0xFF3A86FF);
        parent.addRenderableWidget(progressBar);

        // UI Wizard Multi-Step Engine Layout Switches
        switch (this.currentStep) {
            case WELCOME -> {
                StyledButton continueBtn = new StyledButton(x + 15, y + 85, 80, 20, Component.translatable("screen.logistics.generic.continue"), () -> {
                    this.currentStep = Step.ROOT_PASSWORD;
                    if (parent instanceof TabbedScreen tabbedScreen) {
                        tabbedScreen.refreshCurrentTab();
                    }
                });
                parent.addRenderableWidget(continueBtn);
            }
            case ROOT_PASSWORD -> {
                // Left field: Enter Password
                this.passwordField = new EditBox(font, x + 15, y + 52, 150, 20, Component.translatable("screen.logistics.generic.password"));
                this.passwordField.setMaxLength(32);
                this.passwordField.setValue(this.rootPassword); // Populates existing cached values
                this.passwordField.setFormatter((text, bg) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
                parent.addRenderableWidget(this.passwordField);

                // Right field: Confirm Password
                this.confirmField = new EditBox(font, x + 180, y + 52, 150, 20, Component.translatable("screen.logistics.generic.password.confirm"));
                this.confirmField.setMaxLength(32);
                this.confirmField.setValue(this.confirmPassword); // Populates existing cached values
                this.confirmField.setFormatter((text, bg) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
                parent.addRenderableWidget(this.confirmField);

                // Back Button: Saves current input state text even if invalid, then steps back
                StyledButton backBtn = new StyledButton(x + 15, y + 88, 80, 20, Component.translatable("screen.logistics.generic.back"), () -> {
                    savePasswordInputs();
                    this.currentStep = Step.WELCOME;
                    if (parent instanceof TabbedScreen tabbedScreen) {
                        tabbedScreen.refreshCurrentTab();
                    }
                });
                parent.addRenderableWidget(backBtn);

                // Continue Button
                StyledButton continueBtn = new StyledButton(x + 100, y + 88, 80, 20, Component.translatable("screen.logistics.generic.continue"), () -> {
                    savePasswordInputs();
                    if (!this.rootPassword.isEmpty() && this.rootPassword.equals(this.confirmPassword)) {
                        this.currentStep = Step.SYSTEM_NAME;
                        if (parent instanceof TabbedScreen tabbedScreen) {
                            tabbedScreen.refreshCurrentTab();
                        }
                    }
                });
                parent.addRenderableWidget(continueBtn);
            }
            case SYSTEM_NAME -> {
                this.hostnameField = new EditBox(font, x + 15, y + 52, 150, 20, Component.translatable("screen.logistics.generic.hostname"));
                this.hostnameField.setMaxLength(32);
                this.hostnameField.setValue(this.systemName); // Populates existing cached values
                parent.addRenderableWidget(this.hostnameField);

                // Back Button
                StyledButton backBtn = new StyledButton(x + 15, y + 88, 80, 20, Component.translatable("screen.logistics.generic.back"), () -> {
                    saveHostnameInput();
                    this.currentStep = Step.ROOT_PASSWORD;
                    if (parent instanceof TabbedScreen tabbedScreen) {
                        tabbedScreen.refreshCurrentTab();
                    }
                });
                parent.addRenderableWidget(backBtn);

                // Continue Button
                StyledButton continueBtn = new StyledButton(x + 100, y + 88, 80, 20, Component.translatable("screen.logistics.generic.continue"), () -> {
                    saveHostnameInput();
                    if (!this.systemName.trim().isEmpty()) {
                        this.currentStep = Step.SUMMARY;
                        if (parent instanceof TabbedScreen tabbedScreen) {
                            tabbedScreen.refreshCurrentTab();
                        }
                    }
                });
                parent.addRenderableWidget(continueBtn);
            }
            case SUMMARY -> {
                // Back Button
                StyledButton backBtn = new StyledButton(x + 15, y + 105, 80, 20, Component.translatable("screen.logistics.generic.back"), () -> {
                    this.currentStep = Step.SYSTEM_NAME;
                    if (parent instanceof TabbedScreen tabbedScreen) {
                        tabbedScreen.refreshCurrentTab();
                    }
                });
                parent.addRenderableWidget(backBtn);

                StyledButton installBtn = new StyledButton(x + 100, y + 105, 80, 20, Component.translatable("screen.logistics.generic.install"), () -> {
                    ClientNetworking.sendToServer(new ComputerInstallPacket(ComputerScreen.getComputerNode().getBlockPos(), this.systemName, this.rootPassword));
                    this.currentStep = Step.ANIMATION;
                    this.animationTicks = 0;
                    this.animationProgress = 0.0f;
                    this.pauseTicks = 0;
                    this.nextSplashUpdateTick = 0;
                    updateSplashText();
                    if (parent instanceof TabbedScreen tabbedScreen) {
                        tabbedScreen.refreshCurrentTab();
                    }
                });
                parent.addRenderableWidget(installBtn);
            }
        }
    }

    // Helper routines to flush values into tracking fields before destroying step widgets
    private void savePasswordInputs() {
        if (this.passwordField != null) this.rootPassword = this.passwordField.getValue();
        if (this.confirmField != null) this.confirmPassword = this.confirmField.getValue();
    }

    private void saveHostnameInput() {
        if (this.hostnameField != null) this.systemName = this.hostnameField.getValue();
    }

    @Override
    public void tick() {
        if (this.passwordField != null) this.passwordField.tick();
        if (this.confirmField != null) this.confirmField.tick();
        if (this.hostnameField != null) this.hostnameField.tick();
        if (this.currentStep == Step.ANIMATION) {
            this.animationTicks++;

            // 120 client ticks = 6 seconds absolute execution duration ceiling
            if (this.animationTicks >= 120) {
                this.animationProgress = 1.0f;
                ComputerScreen.open(Minecraft.getInstance(), new OpenComputerPacket(ComputerScreen.getComputerNode().getBlockPos()));
                return;
            }

            java.util.Random random = java.util.concurrent.ThreadLocalRandom.current();

            // Rotate active installation text every 1 to 2 seconds at random intervals
            if (this.animationTicks >= this.nextSplashUpdateTick) {
                updateSplashText();
                this.nextSplashUpdateTick = this.animationTicks + 20 + random.nextInt(20);
            }

            // Fake random execution pauses engine
            if (this.pauseTicks > 0) {
                this.pauseTicks--;
            } else {
                // 5% chance per tick to freeze progress for 10-25 ticks if below 85% total complete
                if (random.nextFloat() < 0.05f && this.animationProgress < 0.85f) {
                    this.pauseTicks = 10 + random.nextInt(15);
                } else {
                    // Smooth progressive increment scaled dynamically against remaining time
                    float ticksRemaining = 120 - this.animationTicks;
                    if (ticksRemaining > 0) {
                        float progressRemaining = 1.0f - this.animationProgress;
                        float expectedStep = progressRemaining / ticksRemaining;
                        // Apply noise variation to the step speed to make the progression look organic
                        float noise = 1.0f + (random.nextFloat() * 0.6f - 0.3f);
                        this.animationProgress = Math.min(0.99f, this.animationProgress + (expectedStep * noise));
                    }
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta, int x, int y, int width, int height) {
        Font font = Minecraft.getInstance().font;

        int currentX = x + 15;
        int currentY = y + 15;
        int contentWidth = width - 30;

        if (this.currentStep != Step.ANIMATION) {
            String stepCounterStr = (currentStep.ordinal() + 1) + " / " + (Step.values().length - 1) + " " + Component.translatable("screen.logistics.computer.tab.install.steps").getString();
            gfx.drawString(font, stepCounterStr, x + width - 15 - font.width(stepCounterStr), y + height - 19, 0xFFAAAAAA, false);
        }

        switch (this.currentStep) {
            case WELCOME -> {
                currentY = renderH1(gfx, font, Component.translatable("screen.logistics.computer.tab.install.installer"), new ItemStack(BlockRegistry.COMPUTER_BLOCK.getBlock()), currentX, currentY);
                currentY += 4;
                currentY = renderHorizontalLine(gfx, currentX, currentY, contentWidth, 0x44FFFFFF);
                currentY += 6;
                renderParagraph(gfx, font, Component.translatable("screen.logistics.computer.tab.install.welcome"), currentX, currentY, contentWidth, 0xFFFFFFFF);
            }
            case ROOT_PASSWORD -> {
                currentY = renderH1(gfx, font, Component.translatable("screen.logistics.computer.tab.install.set_credentials"), null, currentX, currentY);
                currentY += 4;
                currentY = renderHorizontalLine(gfx, currentX, currentY, contentWidth, 0x44FFFFFF);
                currentY += 6;

                gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.install.root_password").getString(), x + 15, y + 42, 0xFFAAAAAA, false);
                gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.install.confirm_password").getString(), x + 180, y + 42, 0xFFAAAAAA, false);

                if (this.passwordField != null && this.confirmField != null) {
                    String p1 = this.passwordField.getValue();
                    String p2 = this.confirmField.getValue();
                    if (!p1.isEmpty() && !p2.isEmpty() && !p1.equals(p2)) {
                        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.install.password_not_matching").getString(), x + 190, y + 94, 0xFFFF5555, false);
                    }
                }
            }
            case SYSTEM_NAME -> {
                currentY = renderH1(gfx, font, Component.translatable("screen.logistics.generic.hostname"), null, currentX, currentY);
                currentY += 4;
                currentY = renderHorizontalLine(gfx, currentX, currentY, contentWidth, 0x44FFFFFF);
                currentY += 6;

                gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.install.asign_admin").getString(), x + 15, y + 42, 0xFFAAAAAA, false);
            }
            case SUMMARY -> {
                currentY = renderH1(gfx, font, Component.translatable("screen.logistics.computer.tab.install.ready"), null, currentX, currentY);
                currentY += 4;
                currentY = renderHorizontalLine(gfx, currentX, currentY, contentWidth, 0x44FFFFFF);
                currentY += 6;

                currentY = renderParagraph(gfx, font, Component.translatable("screen.logistics.computer.tab.install.params_ready"), currentX, currentY, contentWidth, 0xFFFFFFFF);
                currentY += 6;
                gfx.drawString(font, Component.translatable("screen.logistics.generic.hostname").getString() + ": " + this.systemName, currentX, currentY, 0xFF55FF55, false);
                currentY += 12;
                gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.install.root_password_configured").getString(), currentX, currentY, 0xFF55FF55, false);
            }
            case ANIMATION -> {
                currentY = renderH1(gfx, font, Component.translatable("screen.logistics.computer.tab.install.installing"), null, currentX, currentY);
                currentY += 4;
                currentY = renderHorizontalLine(gfx, currentX, currentY, contentWidth, 0x44FFFFFF);
                currentY += 20;

                // Render current active vanilla Minecraft splash message string
                gfx.drawString(font, this.currentSplashText, currentX, currentY, 0xFFAAAAAA, false);
                currentY += 14;

                // Configuration boundary variables for the installation progress bar line
                int barX = currentX;
                int barY = currentY;
                int barW = contentWidth;
                int barH = 12;

                // Progress Bar Outer Box Outline Frame & Backplate
                gfx.fill(barX, barY, barX + barW, barY + barH, 0xFF000000);
                gfx.fill(barX, barY, barX + barW, barY + 1, 0xFF2A2A2A);
                gfx.fill(barX, barY + barH - 1, barX + barW, barY + barH, 0xFF2A2A2A);
                gfx.fill(barX, barY, barX + 1, barY + barH, 0xFF2A2A2A);
                gfx.fill(barX + barW - 1, barY, barX + barW, barY + barH, 0xFF2A2A2A);

                // Progress Inner Active Bar Filler Fill Graphic
                int innerWidth = barW - 2;
                int filledWidth = (int) (innerWidth * this.animationProgress);
                if (filledWidth > 0) {
                    gfx.fill(barX + 1, barY + 1, barX + 1 + filledWidth, barY + barH - 1, 0xFF3A86FF);
                }

                // Render Percentage readouts attached right above the fill meter
                String percentString = (int) (this.animationProgress * 100) + "%";
                gfx.drawString(font, percentString, barX + barW - font.width(percentString), barY - 11, 0xFF3A86FF, false);
            }
        }
    }

    private void updateSplashText() {
        var splashManager = Minecraft.getInstance().getSplashManager();
        SplashRenderer splashInstance = splashManager.getSplash();
        if (splashInstance != null) {
            this.currentSplashText = splashInstance.splash;
            return;
        }
        this.currentSplashText = Component.translatable("screen.logistics.computer.tab.install.unpacking").getString();
    }

    private int renderH1(GuiGraphics gfx, Font font, Component text, ItemStack icon, int x, int y) {
        float scale = 1.2f;
        int iconSize = 16;
        int textXOffset = x;

        if (icon != null && !icon.isEmpty()) {
            gfx.renderFakeItem(icon, x, y);
            textXOffset += iconSize + 6;
        }

        gfx.pose().pushPose();
        gfx.pose().translate(textXOffset, y, 0);
        gfx.pose().scale(scale, scale, 1.0f);
        gfx.drawString(font, text, 0, 0, 0xFFFFFFFF, true);
        gfx.pose().popPose();

        int totalHeight = Math.max((int) (font.lineHeight * scale), icon != null && !icon.isEmpty() ? iconSize : 0);
        return y + totalHeight + 6;
    }

    private int renderParagraph(GuiGraphics gfx, Font font, Component text, int x, int y, int wrapWidth, int color) {
        List<FormattedCharSequence> splitLines = font.split(text, wrapWidth);
        for (FormattedCharSequence line : splitLines) {
            gfx.drawString(font, line, x, y, color, false);
            y += font.lineHeight + 2;
        }
        return y;
    }

    private int renderHorizontalLine(GuiGraphics gfx, int x, int y, int width, int color) {
        gfx.fill(x, y, x + width, y + 1, color);
        return y + 1;
    }
}
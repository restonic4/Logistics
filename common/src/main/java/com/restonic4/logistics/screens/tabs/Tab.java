package com.restonic4.logistics.screens.tabs;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class Tab {
    protected final Component title;
    protected ResourceLocation leftIcon;
    protected ResourceLocation rightIcon;
    protected TabButtonRenderer customButtonRenderer;

    protected Tab(Component title) {
        this.title = title;
    }

    public Component getTitle() {
        return title;
    }

    public Tab withLeftIcon(ResourceLocation icon) {
        this.leftIcon = icon;
        return this;
    }

    public Tab withRightIcon(ResourceLocation icon) {
        this.rightIcon = icon;
        return this;
    }

    public Tab withIcons(ResourceLocation left, ResourceLocation right) {
        this.leftIcon = left;
        this.rightIcon = right;
        return this;
    }

    public Tab withButtonRenderer(TabButtonRenderer renderer) {
        this.customButtonRenderer = renderer;
        return this;
    }

    public ResourceLocation getLeftIcon() {
        return leftIcon;
    }

    public ResourceLocation getRightIcon() {
        return rightIcon;
    }

    public TabButtonRenderer getCustomButtonRenderer() {
        return customButtonRenderer;
    }

    public abstract void init(Screen parent, int x, int y, int width, int height);

    public abstract void tick();

    public abstract void render(GuiGraphics gfx, int mouseX, int mouseY, float delta, int x, int y, int width, int height);

    public void onShow() {}

    public void onHide() {}

    public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseDragged(double mouseX, double mouseY, double dragX, double dragY, int button) { return false; }
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) { return false; }
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    public boolean charTyped(char codePoint, int modifiers) { return false; }
}
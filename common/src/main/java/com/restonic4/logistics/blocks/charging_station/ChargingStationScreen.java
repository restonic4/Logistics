package com.restonic4.logistics.blocks.charging_station;

import com.restonic4.logistics.networking.ClientNetworking;
import com.restonic4.logistics.screens.widgets.ProgressBarWidget;
import com.restonic4.logistics.screens.widgets.StyledButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ChargingStationScreen extends Screen {
    private final BlockPos pos;
    private ItemStack displayedStack = ItemStack.EMPTY;
    private long storedEnergy = 0;
    private long maxEnergy = 0;

    private int panelLeft, panelTop;
    private static final int PANEL_W = 220;
    private static final int PANEL_H = 150;

    private int slotX, slotY;
    private static final int SLOT_SIZE = 18;

    private ProgressBarWidget progressBar;
    private StyledButton extractButton;
    private int syncCooldown = 0;

    public ChargingStationScreen(BlockPos pos) {
        super(Component.literal("Charging Station"));
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }

    public void update(ItemStack stack, long stored, long max) {
        this.displayedStack = stack;
        this.storedEnergy = stored;
        this.maxEnergy = max;
    }

    @Override
    protected void init() {
        calculateBounds();

        int barW = 120;
        int barH = 10;
        int barX = panelLeft + (PANEL_W - barW) / 2;
        int barY = slotY + SLOT_SIZE + 18;

        progressBar = new ProgressBarWidget(barX, barY, barW, barH, 0);
        progressBar.setRange(0, 100);
        progressBar.setBarColor(0xFF00FF55);
        addRenderableWidget(progressBar);

        int btnW = 80;
        int btnH = 20;
        int btnX = panelLeft + (PANEL_W - btnW) / 2;
        int btnY = barY + barH + 20;

        extractButton = new StyledButton(btnX, btnY, btnW, btnH, Component.literal("Extract"), () -> {
            sendAction(ChargingStationInteractPacket.Action.EXTRACT);
        });
        addRenderableWidget(extractButton);

        sendAction(ChargingStationInteractPacket.Action.SYNC);
    }

    private void calculateBounds() {
        panelLeft = (this.width - PANEL_W) / 2;
        panelTop = (this.height - PANEL_H) / 2;
        slotX = panelLeft + (PANEL_W - SLOT_SIZE) / 2;
        slotY = panelTop + 32;
    }

    @Override
    public void tick() {
        super.tick();
        if (syncCooldown-- <= 0) {
            syncCooldown = 20;
            sendAction(ChargingStationInteractPacket.Action.SYNC);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);

        // Panel
        gfx.fill(panelLeft, panelTop, panelLeft + PANEL_W, panelTop + PANEL_H, 0xFF121212);
        gfx.fill(panelLeft + 1, panelTop + 1, panelLeft + PANEL_W - 1, panelTop + PANEL_H - 1, 0xFF1A1A1A);
        gfx.renderOutline(panelLeft, panelTop, PANEL_W, PANEL_H, 0xFF2A2A2A);

        // Title
        int titleW = font.width(title);
        gfx.drawString(font, title, panelLeft + (PANEL_W - titleW) / 2, panelTop + 8, 0xFFFFFFFF);

        // Slot
        gfx.renderOutline(slotX, slotY, SLOT_SIZE, SLOT_SIZE, 0xFF2A2A2A);
        gfx.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, 0xFF0A0A0A);

        if (!displayedStack.isEmpty()) {
            gfx.renderItem(displayedStack, slotX + 1, slotY + 1);
            gfx.renderItemDecorations(font, displayedStack, slotX + 1, slotY + 1);

            if (maxEnergy > 0) {
                int pct = (int) ((storedEnergy * 100L) / maxEnergy);
                progressBar.setValue(pct);
            }
            progressBar.visible = true;

            String txt = storedEnergy + " / " + maxEnergy + " EU";
            int txtW = font.width(txt);
            gfx.drawString(font, txt, panelLeft + (PANEL_W - txtW) / 2, progressBar.getY() + progressBar.getHeight() + 4, 0xFFAAAAAA);

            extractButton.visible = true;
        } else {
            progressBar.visible = false;
            extractButton.visible = false;
            String empty = "Empty";
            int ew = font.width(empty);
            gfx.drawString(font, empty, panelLeft + (PANEL_W - ew) / 2, slotY + 5, 0xFF777777);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
            if (displayedStack.isEmpty()) {
                ItemStack hand = Minecraft.getInstance().player.getMainHandItem();
                if (EnergyItemHelper.isEnergyItem(hand)) {
                    sendAction(ChargingStationInteractPacket.Action.INSERT);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void sendAction(ChargingStationInteractPacket.Action action) {
        ClientNetworking.sendToServer(new ChargingStationInteractPacket(pos, action));
    }
}
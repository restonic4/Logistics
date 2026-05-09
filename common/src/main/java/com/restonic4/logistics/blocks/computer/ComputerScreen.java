package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.networking.ClientNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ComputerScreen extends Screen {
    private static List<BlockPos> accessors = new ArrayList<>();

    private BlockPos fromPos = null;
    private BlockPos targetPos = null;

    private EditBox quantityBox;
    private EditBox textBox;
    private Button transferButton;

    public ComputerScreen() {
        super(Component.literal("Computer"));
    }

    public static void setAccessors(ComputerSyncPacket payload) {
        accessors = payload.accessors();
    }

    private void executeTransfer() {
        String qty = quantityBox.getValue();
        String extraText = textBox.getValue();

        ClientNetworking.sendToServer(new ComputerTransferPacket(fromPos, targetPos, Integer.parseInt(qty), extraText));
    }

    @Override
    protected void init() {
        int xMid = this.width / 2;

        this.quantityBox = new EditBox(this.font, xMid - 105, this.height - 40, 100, 20, Component.literal("Quantity"));
        this.quantityBox.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.addRenderableWidget(this.quantityBox);

        this.textBox = new EditBox(this.font, xMid + 5, this.height - 40, 100, 20, Component.literal("Data"));
        this.addRenderableWidget(this.textBox);

        this.transferButton = Button.builder(Component.literal("Transfer"), (btn) -> {
            executeTransfer();
        }).bounds(xMid - 50, this.height - 70, 100, 20).build();
        this.addRenderableWidget(this.transferButton);
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        renderBackground(gfx);

        // List Header
        gfx.drawCenteredString(this.font, "Accessors List (L: From | R: Target)", this.width / 2, 10, 0xFFFFFF);

        // Render the list of accessors
        int startY = 30;
        int itemHeight = 14;

        for (int i = 0; i < accessors.size(); i++) {
            BlockPos pos = accessors.get(i);
            int yPos = startY + (i * itemHeight);
            int xPos = this.width / 2 - 100;

            int color = 0x44FFFFFF;
            if (pos.equals(fromPos)) color = 0x8800FF00;
            if (pos.equals(targetPos)) color = 0x880000FF;

            gfx.fill(xPos, yPos, xPos + 200, yPos + itemHeight - 2, color);
            gfx.drawString(this.font, pos.toShortString(), xPos + 5, yPos + 2, 0xFFFFFF);
        }

        super.render(gfx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startY = 30;
        int itemHeight = 14;
        int xPos = this.width / 2 - 100;

        for (int i = 0; i < accessors.size(); i++) {
            int yPos = startY + (i * itemHeight);

            if (mouseX >= xPos && mouseX <= xPos + 200 && mouseY >= yPos && mouseY <= yPos + itemHeight - 2) {
                BlockPos clickedPos = accessors.get(i);

                if (button == 0) {
                    fromPos = clickedPos;
                } else if (button == 1) {
                    targetPos = clickedPos;
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.computer.ComputerSyncPacket;
import com.restonic4.logistics.blocks.computer.ComputerTransferPacket;
import com.restonic4.logistics.networking.ClientNetworking;
import com.restonic4.logistics.screens.tabs.Tab;
import com.restonic4.logistics.screens.widgets.NumberPickerWidget;
import com.restonic4.logistics.screens.widgets.SearchableDropdownWidget;
import com.restonic4.logistics.screens.widgets.StyledButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransferTab extends Tab {
    private NumberPickerWidget quantityBox;
    private SearchableDropdownWidget<String> textBox;
    private SearchableDropdownWidget<BlockPos> leftAccessor;
    private SearchableDropdownWidget<BlockPos> rightAccessor;
    private StyledButton transferButton;

    // Persisted state across tab switches / resizes
    private int savedQuantity = 1;
    private String savedItem = "minecraft:chest";
    private BlockPos savedLeft = null;
    private BlockPos savedRight = null;

    public TransferTab() {
        super(Component.translatable("screen.logistics.computer.tab.transfer.title"));
    }

    @Override
    public void init(Screen parent, int x, int y, int width, int height) {
        int midW = 50;
        int midX = x + width / 2 - midW / 2;
        int midY = y + height / 2 - midW / 2 - 10;

        // Quantity
        this.quantityBox = new NumberPickerWidget(midX, midY + midW + 8, midW, 20, Component.empty(), 1, val -> {});
        this.quantityBox.setRange(1, 64);
        this.quantityBox.setValue(savedQuantity);
        parent.addRenderableWidget(this.quantityBox);

        // Item selector
        this.textBox = new SearchableDropdownWidget<>(midX, midY, midW, midW, Component.empty(), new ArrayList<>(), val -> {});
        this.textBox.setSelectionRenderer(new SearchableDropdownWidget.CompactItemSelectorRenderer<>(4, 0.5f));
        if (savedItem != null) this.textBox.setSelectedValue(savedItem);
        parent.addRenderableWidget(this.textBox);

        // FROM
        this.leftAccessor = new SearchableDropdownWidget<>(x + 20, midY, midW, midW, Component.empty(), new ArrayList<>(), val -> {});
        this.leftAccessor.setSelectionRenderer(new SearchableDropdownWidget.CompactItemSelectorRenderer<>(4, 0.5f));
        parent.addRenderableWidget(this.leftAccessor);

        // TARGET
        this.rightAccessor = new SearchableDropdownWidget<>(x + width - midW - 20, midY, midW, midW, Component.empty(), new ArrayList<>(), val -> {});
        this.rightAccessor.setSelectionRenderer(new SearchableDropdownWidget.CompactItemSelectorRenderer<>(4, 0.5f));
        parent.addRenderableWidget(this.rightAccessor);

        // Transfer button
        this.transferButton = new StyledButton(
                x + width / 2 - 50,
                y + height - 30,
                100,
                20,
                Component.translatable("screen.logistics.computer.tab.transfer.title"),
                this::executeTransfer
        );
        parent.addRenderableWidget(this.transferButton);

        refreshAccessorDropdowns();
    }

    public void refreshAccessorDropdowns() {
        List<ComputerSyncPacket.AccessorData> accessors = ComputerScreen.getAccessors();

        List<SearchableDropdownWidget.DropdownEntry<BlockPos>> leftEntries = new ArrayList<>();
        List<SearchableDropdownWidget.DropdownEntry<BlockPos>> rightEntries = new ArrayList<>();
        List<SearchableDropdownWidget.DropdownEntry<String>> itemEntries = new ArrayList<>();

        leftEntries.add(new SearchableDropdownWidget.DropdownEntry<>(null,
                Component.translatable("screen.logistics.computer.tab.transfer.auto"),
                SearchableDropdownWidget.DropdownIcon.of(com.restonic4.logistics.experiment.Items.CHIP.getItem())));

        Set<String> addedItemIds = new HashSet<>();

        for (ComputerSyncPacket.AccessorData accessorData : accessors) {
            BlockPos pos = accessorData.pos();

            leftEntries.add(new SearchableDropdownWidget.DropdownEntry<>(pos,
                    Component.literal(pos.toShortString()),
                    SearchableDropdownWidget.DropdownIcon.of(BlockRegistry.ACCESSOR_BLOCK.getBlock())));

            rightEntries.add(new SearchableDropdownWidget.DropdownEntry<>(pos,
                    Component.literal(pos.toShortString()),
                    SearchableDropdownWidget.DropdownIcon.of(BlockRegistry.ACCESSOR_BLOCK.getBlock())));

            for (ItemStack itemStack : accessorData.inventory()) {
                if (itemStack.getItem() == Items.AIR) continue;
                ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
                String itemId = registryName.toString();

                if (!addedItemIds.contains(itemId)) {
                    addedItemIds.add(itemId);
                    itemEntries.add(new SearchableDropdownWidget.DropdownEntry<>(itemId,
                            Component.translatable(itemStack.getItem().getDescriptionId()),
                            SearchableDropdownWidget.DropdownIcon.of(itemStack)));
                }
            }
        }

        if (leftAccessor != null) {
            leftAccessor.setOptions(leftEntries);
            leftAccessor.setSelectedValue(savedLeft);
        }

        if (rightAccessor != null) {
            rightAccessor.setOptions(rightEntries);
            if (savedRight != null) rightAccessor.setSelectedValue(savedRight);
        }

        if (textBox != null) {
            textBox.setOptions(itemEntries);
        }
    }

    private void executeTransfer() {
        BlockPos from = leftAccessor != null ? leftAccessor.getSelectedValue() : null;
        BlockPos target = rightAccessor != null ? rightAccessor.getSelectedValue() : null;
        if (target == null) return;

        int qty = quantityBox != null ? (int) quantityBox.getValue() : 1;
        String extra = textBox != null ? textBox.getSelectedValue() : null;

        savedQuantity = qty;
        savedItem = extra;
        savedLeft = from;
        savedRight = target;

        ClientNetworking.sendToServer(new ComputerTransferPacket(ComputerScreen.getComputerNode(), from, target, qty, extra != null ? extra : ""));
    }

    @Override
    public void onHide() {
        if (quantityBox != null) savedQuantity = (int) quantityBox.getValue();
        if (textBox != null) savedItem = textBox.getSelectedValue();
        if (leftAccessor != null) savedLeft = leftAccessor.getSelectedValue();
        if (rightAccessor != null) savedRight = rightAccessor.getSelectedValue();
    }

    @Override
    public void tick() {
        if (quantityBox != null) quantityBox.tick();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta, int x, int y, int width, int height) {
        renderArrow(gfx, x, y, width, height);
    }

    private void renderArrow(GuiGraphics gfx, int x, int y, int width, int height) {
        int midW = 50;
        int midY = y + height / 2 - midW / 2 - 10;

        int startX = (x + 20) + midW + 8;
        int endX = (x + width - midW - 20) - 8;
        int cy = midY + midW / 2;
        if (endX <= startX) return;

        int color = 0xFFFFFFFF;
        int shaftThick = 6;
        int headLen = 12;
        int headHalf = 7;
        int shaftEnd = endX - headLen;

        gfx.fill(startX, cy - shaftThick / 2, shaftEnd, cy + shaftThick / 2, color);

        for (int i = 0; i < headLen; i++) {
            int px = shaftEnd + i;
            int halfH = (headHalf * (headLen - i) + headLen / 2) / headLen;
            if (halfH < 1) halfH = 1;
            gfx.fill(px, cy - halfH, px + 1, cy + halfH, color);
        }
    }
}
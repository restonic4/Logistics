package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.blocks.computer.ComputerScreenOffPacket;
import com.restonic4.logistics.blocks.computer.ComputerSyncPacket;
import com.restonic4.logistics.networking.ClientNetworking;
import com.restonic4.logistics.screens.tabs.TabDistribution;
import com.restonic4.logistics.screens.tabs.TabbedScreen;
import com.restonic4.logistics.screens.widgets.StyledButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;

public class ComputerScreen extends TabbedScreen {
    private static BlockPos computerNode;
    private static List<ComputerSyncPacket.AccessorData> accessors = new ArrayList<>();

    private final TransferTab transferTab;
    private final LogTab logTab;
    private final InfoTab testTab;

    // === Fullscreen state ===
    private boolean maximized = false;
    private static final int DEFAULT_PANEL_WIDTH = 388;
    private static final int DEFAULT_PANEL_HEIGHT = 240;

    private SimpleSoundInstance ambientSoundInstance;

    public ComputerScreen() {
        super(Component.literal("Computer"));

        this.distribution = TabDistribution.FILL_NO_PAGES;
        this.tabGap = 2;

        this.transferTab = new TransferTab();
        addTab(transferTab.withLeftIcon(new ResourceLocation("logistics", "textures/item/parcel.png")));
        this.logTab = new LogTab();
        addTab(logTab.withLeftIcon(new ResourceLocation("logistics", "textures/item/chip.png")));
        this.testTab = new InfoTab();
        addTab(testTab.withLeftIcon(new ResourceLocation("logistics", "textures/item/chip.png")));
    }

    @Override
    protected void init() {
        super.init();

        if (this.ambientSoundInstance == null && this.minecraft != null && this.minecraft.player != null) {
            this.ambientSoundInstance = new SimpleSoundInstance(
                    new ResourceLocation("logistics", "computer/ambient"),
                    SoundSource.BLOCKS,
                    0.5F, // Volume
                    1.0F, // Pitch
                    SoundInstance.createUnseededRandom(),
                    true, // Looping -> true
                    0,    // Delay
                    SoundInstance.Attenuation.NONE, // No attenuation means it plays at full volume regardless of player distance/movement
                    0.0D, 0.0D, 0.0D, // X, Y, Z coordinates
                    true  // Relative to player
            );

            this.minecraft.getSoundManager().play(this.ambientSoundInstance);
        }
    }

    @Override
    protected void calculateBounds() {
        // If maximized, fill the entire Minecraft window
        if (maximized) {
            this.panelWidth = this.width;
            this.panelHeight = this.height;
        } else {
            this.panelWidth = DEFAULT_PANEL_WIDTH;
            this.panelHeight = DEFAULT_PANEL_HEIGHT;
        }

        super.calculateBounds();
        this.tabBarRightReserve = (calculateCloseButtonSize() + tabGap) * 3 + tabGap;
    }

    @Override
    protected void addPersistentWidgets() {
        int btnH = calculateCloseButtonSize();
        int btnY = ((panelTop + contentTop) / 2) - btnH / 2;
        int btnX = panelRight - btnH - (btnY - panelTop);

        // Close
        StyledButton close = new StyledButton(
                btnX, btnY, btnH, btnH,
                Component.literal("X"),
                this::onClose
        );
        close.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF)
                .withHoverColor(0xFF252525)
                .withPressColor(0xFF303030);
        this.addRenderableWidget(close);

        // Maximize / Restore (<>)
        StyledButton maximize = new StyledButton(
                btnX - (btnH + tabGap), btnY, btnH, btnH,
                Component.literal(maximized ? "[]" : "<>"),
                this::toggleMaximized
        );
        maximize.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF)
                .withHoverColor(0xFF252525)
                .withPressColor(0xFF303030);
        this.addRenderableWidget(maximize);

        // Minimize / Restore default size (-)
        StyledButton small = new StyledButton(
                btnX - (btnH + tabGap) * 2, btnY, btnH, btnH,
                Component.literal("-"),
                this::onClose
        );
        small.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF)
                .withHoverColor(0xFF252525)
                .withPressColor(0xFF303030);
        this.addRenderableWidget(small);
    }

    private int calculateCloseButtonSize() {
        return contentTop - (panelTop + 2) - 4;
    }

    /** Toggle between fullscreen and default window size. */
    private void toggleMaximized() {
        maximized = !maximized;
        refreshLayout();
    }

    /** Force back to the small default panel. */
    private void restoreSize() {
        if (maximized) {
            maximized = false;
            refreshLayout();
        }
    }

    /** Recalculate geometry and rebuild all tabs/widgets without losing state. */
    private void refreshLayout() {
        calculateBounds();
        rebuildTabBar();
    }

    public LogTab getLogTab() {
        return logTab;
    }

    public static void setAccessors(ComputerSyncPacket payload) {
        computerNode = payload.computerNode();
        accessors = new ArrayList<>(payload.accessors());

        if (Minecraft.getInstance().screen instanceof ComputerScreen screen) {
            screen.transferTab.refreshAccessorDropdowns();
        }
    }

    public static BlockPos getComputerNode() {
        return computerNode;
    }
    public static List<ComputerSyncPacket.AccessorData> getAccessors() {
        return accessors;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // Let widgets (dropdowns, number pickers) handle scroll first
        if (super.mouseScrolled(mouseX, mouseY, amount)) return true;

        // Then offer it to the log tab if it's currently selected
        if (selectedTab >= 0 && tabs.get(selectedTab) == logTab) {
            return logTab.handleMouseScrolled(
                    mouseX, mouseY, amount,
                    contentLeft, contentTop,
                    contentRight - contentLeft,
                    contentBottom - contentTop
            );
        }
        return false;
    }

    @Override
    public void onClose() {
        if (computerNode != null) {
            ClientNetworking.sendToServer(new ComputerScreenOffPacket(computerNode));
        }
        stopAmbientSound();
        super.onClose();
    }

    private void stopAmbientSound() {
        if (this.ambientSoundInstance != null && this.minecraft != null) {
            this.minecraft.getSoundManager().stop(this.ambientSoundInstance);
            this.ambientSoundInstance = null;
        }
    }
}
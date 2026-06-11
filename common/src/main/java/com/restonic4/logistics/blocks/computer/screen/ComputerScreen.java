package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.blocks.computer.ComputerNode;
import com.restonic4.logistics.blocks.computer.ComputerScreenOffPacket;
import com.restonic4.logistics.blocks.computer.OpenComputerPacket;
import com.restonic4.logistics.blocks.computer.screen.triggers.TriggersTab;
import com.restonic4.logistics.networking.ClientNetworking;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.client.ClientNetworkManager;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.screens.tabs.TabDistribution;
import com.restonic4.logistics.screens.tabs.TabbedScreen;
import com.restonic4.logistics.screens.widgets.StyledButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

public class ComputerScreen extends TabbedScreen {
    private static EnergyNetwork energyNetwork;
    private static ComputerNode computerNode;

    private boolean isLoggedIn = false;

    private final TransferTab transferTab;
    private final ProtectionTab protectionTab;
    private final LogTab logTab;
    private final AudioTab audioTab;
    private final TriggersTab triggersTab;
    private final InstallTab installTab;
    private final LoginTab loginTab;

    private boolean maximized = false;
    private static final int DEFAULT_PANEL_WIDTH = 388;
    private static final int DEFAULT_PANEL_HEIGHT = 240;

    private SimpleSoundInstance ambientSoundInstance;

    public ComputerScreen() {
        super(Component.translatable("screen.logistics.computer.title"));

        this.distribution = TabDistribution.FILL_NO_PAGES;
        this.tabGap = 2;

        this.transferTab = new TransferTab();
        transferTab.withLeftIcon(new ResourceLocation("logistics", "textures/item/parcel.png"));
        this.protectionTab = new ProtectionTab();
        protectionTab.withLeftIcon(new ResourceLocation("logistics", "textures/item/shield.png"));
        this.logTab = new LogTab();
        logTab.withLeftIcon(new ResourceLocation("logistics", "textures/item/paper.png"));
        this.installTab = new InstallTab();
        installTab.withLeftIcon(new ResourceLocation("logistics", "textures/item/parcel.png"));
        this.loginTab = new LoginTab();
        loginTab.withLeftIcon(new ResourceLocation("logistics", "textures/item/chip.png"));
        this.audioTab = new AudioTab();
        audioTab.withLeftIcon(new ResourceLocation("logistics", "textures/item/note_block.png"));
        this.triggersTab = new TriggersTab();
        triggersTab.withLeftIcon(new ResourceLocation("logistics", "textures/item/chip.png"));
    }

    public static void open(Minecraft client, OpenComputerPacket payload) {
        if (client.level == null) return;

        Network network = ClientNetworkManager.getNetwork(client.level.dimension(), payload.computerNodePos());
        if (!(network instanceof EnergyNetwork foundEnergyNetwork)) return;

        NetworkNode node = network.getNodeIndex().findByBlockPos(payload.computerNodePos());
        if (!(node instanceof ComputerNode foundComputerNode)) return;

        energyNetwork = foundEnergyNetwork;
        computerNode = foundComputerNode;

        client.setScreen(new ComputerScreen());
    }

    public static EnergyNetwork getEnergyNetwork() { return energyNetwork; }
    public static ComputerNode getComputerNode() { return computerNode; }

    public void performLogin() {
        this.isLoggedIn = true;
        this.selectedTab = 0;
        this.updateTabs();

        this.clearWidgets();
        this.init(this.minecraft, this.width, this.height);
    }

    public void updateTabs() {
        removeTab(transferTab);
        removeTab(protectionTab);
        removeTab(logTab);
        removeTab(installTab);
        removeTab(loginTab);
        removeTab(audioTab);
        removeTab(triggersTab);

        if (computerNode.isInstalled()) {
            if (isLoggedIn) {
                if (energyNetwork.hasAccessors()) addTab(transferTab);
                if (energyNetwork.hasProtectors()) addTab(protectionTab);
                if (energyNetwork.hasAudioStations()) addTab(audioTab);
                addTab(triggersTab);
                addTab(logTab);
            } else {
                addTab(loginTab);
            }
        } else {
            addTab(installTab);
        }
    }

    @Override
    protected void init() {
        updateTabs();
        super.init();

        if (this.ambientSoundInstance == null && this.minecraft != null && this.minecraft.player != null) {
            this.ambientSoundInstance = new SimpleSoundInstance(
                    new ResourceLocation("logistics", "computer/ambient"),
                    SoundSource.BLOCKS,
                    0.5F,
                    1.0F,
                    SoundInstance.createUnseededRandom(),
                    true,
                    0,
                    SoundInstance.Attenuation.NONE,
                    0.0D, 0.0D, 0.0D,
                    true
            );
            this.minecraft.getSoundManager().play(this.ambientSoundInstance);
        }
    }

    @Override
    protected void calculateBounds() {
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

        StyledButton close = new StyledButton(
                btnX, btnY, btnH, btnH,
                Component.literal("X"),
                this::onClose
        );
        close.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF)
                .withHoverColor(0xFF252525)
                .withPressColor(0xFF303030);
        this.addRenderableWidget(close);

        StyledButton maximize = new StyledButton(
                btnX - (btnH + tabGap), btnY, btnH, btnH,
                Component.literal(maximized ? "[]" : "<>"),
                this::toggleMaximized
        );
        maximize.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF)
                .withHoverColor(0xFF252525)
                .withPressColor(0xFF303030);
        this.addRenderableWidget(maximize);

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

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        super.render(gfx, mouseX, mouseY, delta);
    }

    private int calculateCloseButtonSize() {
        return contentTop - (panelTop + 2) - 4;
    }

    private void toggleMaximized() {
        maximized = !maximized;
        refreshLayout();
    }

    private void restoreSize() {
        if (maximized) {
            maximized = false;
            refreshLayout();
        }
    }

    private void refreshLayout() {
        calculateBounds();
        rebuildTabBar();
    }

    public LogTab getLogTab() {
        return logTab;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (super.mouseScrolled(mouseX, mouseY, amount)) return true;

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
        if (selectedTab >= 0 && selectedTab < tabs.size()) {
            if (tabs.get(selectedTab).onAttemptClose()) {
                return;
            }
        }

        if (computerNode != null) {
            ClientNetworking.sendToServer(new ComputerScreenOffPacket(computerNode.getBlockPos()));
        }
        stopAmbientSound();
        super.onClose();
    }

    @Override
    public void removed() {
        super.removed();
        stopAmbientSound();
    }

    private void stopAmbientSound() {
        if (this.ambientSoundInstance != null && this.minecraft != null) {
            this.minecraft.getSoundManager().stop(this.ambientSoundInstance);
            this.ambientSoundInstance = null;
        }
    }
}
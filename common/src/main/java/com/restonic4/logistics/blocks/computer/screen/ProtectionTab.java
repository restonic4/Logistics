package com.restonic4.logistics.blocks.computer.screen;

import com.mojang.authlib.GameProfile;
import com.restonic4.logistics.blocks.computer.protection.ProtectionEditSyncPacket;
import com.restonic4.logistics.blocks.computer.protection.ProtectionSavePacket;
import com.restonic4.logistics.blocks.protector.data_types.*;
import com.restonic4.logistics.blocks.protector.data_types.ui.EditableProtector;
import com.restonic4.logistics.blocks.protector.data_types.ui.EditableRole;
import com.restonic4.logistics.networking.ClientNetworking;
import com.restonic4.logistics.screens.tabs.Tab;
import com.restonic4.logistics.screens.widgets.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class ProtectionTab extends Tab {

    private int selectedNodeIndex = 0;
    private int selectedRoleIndex = -1;
    private int pendingDeleteIndex = -1;
    private boolean hasUnsavedChanges = false;

    private final List<EditableProtector> protectors = new ArrayList<>();
    private List<GameProfile> playerPool = new ArrayList<>();
    private BlockPos computerPos;

    private AddRolePopup addRolePopup;
    private UnsavedChangesPopup unsavedPopup;
    private ConfirmDeletePopup confirmDeletePopup;

    private ScrollablePanel leftPanel;
    private ScrollablePanel rightPanel;

    private SearchableDropdownWidget<Integer> nodeSelector;
    private final List<RoleEntryWidget> roleEntries = new ArrayList<>();
    private StyledButton addRoleButton;

    private SearchableDropdownWidget<UUID> playerSearchDropdown;
    private StyledButton addPlayerButton;
    private final List<PlayerChipWidget> playerChips = new ArrayList<>();
    private final List<FlagWidget> flagWidgets = new ArrayList<>();
    private StyledButton saveButton;
    private NumberPickerWidget radiusPicker;

    private Screen parent;

    public ProtectionTab() {
        super(Component.translatable("screen.logistics.computer.tab.protector.title"));
    }

    public void receiveSyncData(ProtectionEditSyncPacket packet) {
        this.computerPos = packet.computerPos();
        this.playerPool = packet.allPlayers();
        this.protectors.clear();
        for (ProtectionZone zone : packet.zones()) {
            this.protectors.add(new EditableProtector(zone));
        }

        // CRITICAL: heal client-side so missing flags appear as OFF and are saved back.
        for (EditableProtector ep : this.protectors) {
            for (EditableRole role : ep.roles) {
                for (FlagDefinition def : FlagRegistry.forZone(ep.creative)) {
                    role.flags.putIfAbsent(def.id(), new FlagData(false, ActionType.DENY.name(), 0, ""));
                }
            }
        }

        this.selectedNodeIndex = 0;
        this.selectedRoleIndex = -1;
        this.hasUnsavedChanges = false;
        refreshLeftPanel();
        refreshRightPanel();
    }

    public int getCurrentNodeRadius() {
        if (selectedNodeIndex < 0 || selectedNodeIndex >= protectors.size()) return 0;
        return protectors.get(selectedNodeIndex).radius;
    }

    public int getSelectedNodeIndex() {
        return selectedNodeIndex;
    }

    public BlockPos getComputerNodePos() {
        return computerPos;
    }

    @Override
    public void init(Screen parent, int x, int y, int width, int height) {
        this.parent = parent;

        int leftPanelWidth = Math.max(120, Math.min(160, (int) (width * 0.35)));
        int rightPanelWidth = width - leftPanelWidth - 6;
        int gap = 6;

        leftPanel = new ScrollablePanel(x, y, leftPanelWidth, height);
        leftPanel.setPadding(6);

        rightPanel = new ScrollablePanel(x + leftPanelWidth + gap, y, rightPanelWidth, height);
        rightPanel.setPadding(6);

        buildLeftPanel(leftPanelWidth - 12);
        buildRightPanel(rightPanelWidth - 12);

        if (addRolePopup == null) {
            addRolePopup = new AddRolePopup(parent, this::onRoleCreated, () -> {});
        }
        if (unsavedPopup == null) {
            unsavedPopup = new UnsavedChangesPopup(parent, this::onSaveAndClose, this::onDiscardAndClose, () -> {});
        }

        if (confirmDeletePopup == null) {
            confirmDeletePopup = new ConfirmDeletePopup(parent, this::doConfirmDelete, () -> pendingDeleteIndex = -1);
        }
    }

    private void buildLeftPanel(int innerWidth) {
        int currentY = 0;

        List<SearchableDropdownWidget.DropdownEntry<Integer>> nodeOptions = new ArrayList<>();
        for (int i = 0; i < protectors.size(); i++) {
            EditableProtector ep = protectors.get(i);
            String label = "Protector @ " + ep.pos.toShortString();
            SearchableDropdownWidget.DropdownIcon icon = SearchableDropdownWidget.DropdownIcon.of(Blocks.BEACON);
            nodeOptions.add(new SearchableDropdownWidget.DropdownEntry<>(i, Component.literal(label), icon));
        }

        nodeSelector = new SearchableDropdownWidget<>(
                0, 0, innerWidth, 20,
                Component.empty(), nodeOptions, this::onNodeSelected
        );
        if (selectedNodeIndex >= 0 && selectedNodeIndex < protectors.size()) {
            nodeSelector.setSelectedIndex(selectedNodeIndex);
        }
        leftPanel.addChild(nodeSelector, 0, currentY);
        currentY += 24 + 4;

        roleEntries.clear();
        List<EditableRole> roles = getCurrentRoles();
        for (int i = 0; i < roles.size(); i++) {
            EditableRole role = roles.get(i);
            boolean isDefault = role.type == RoleData.RoleType.DEFAULT;
            int finalI = i;
            RoleEntryWidget entry = new RoleEntryWidget(
                    0, 0, innerWidth, 24,
                    new ResourceLocation(role.iconRl),
                    role.name,
                    isDefault ? null : newName -> onRoleNameChanged(finalI, newName),
                    isDefault ? null : (i > 0 ? () -> onRoleMoveUp(finalI) : null),
                    isDefault ? null : (i < roles.size() - 1 ? () -> onRoleMoveDown(finalI) : null),
                    isDefault ? null : () -> onDeleteRole(finalI),
                    selected -> onRoleSelected(finalI)
            );
            entry.setSelected(i == selectedRoleIndex);
            leftPanel.addChild(entry, 0, currentY);
            roleEntries.add(entry);
            currentY += 24 + 4;
        }

        addRoleButton = new StyledButton(0, 0, innerWidth, 20, Component.literal("+"), this::onAddRoleClicked);
        addRoleButton.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF);
        leftPanel.addChild(addRoleButton, 0, currentY);
    }

    private void buildRightPanel(int innerWidth) {
        innerWidth = Math.max(20, innerWidth - 6);
        int currentY = 0;

        EditableProtector currentProtector = getCurrentProtector();

        rightPanel.addChild(createLabel(Component.translatable("screen.logistics.computer.tab.protector.radius").getString() + ":", innerWidth, 0xFFFFFFFF), 0, currentY);
        currentY += 12;

        radiusPicker = new NumberPickerWidget(
                0, 0, innerWidth, 18,
                Component.empty(),
                getCurrentNodeRadius(),
                this::onRadiusChanged
        );
        radiusPicker.setRange(0, 320);
        radiusPicker.setDecimalPlaces(0);
        radiusPicker.setStep(1.0);
        rightPanel.addChild(radiusPicker, 0, currentY);
        currentY += 18 + 12;

        EditableRole currentRole = getSelectedRole();
        boolean isDefault = currentRole != null && currentRole.type == RoleData.RoleType.DEFAULT;

        rightPanel.addChild(createLabel(Component.translatable("screen.logistics.computer.tab.protector.players").getString() + ":", innerWidth, 0xFFFFFFFF), 0, currentY);
        currentY += 12;

        if (!isDefault && currentRole != null) {
            int searchWidth = innerWidth - 24;
            List<SearchableDropdownWidget.DropdownEntry<UUID>> playerEntries = new ArrayList<>();
            for (GameProfile profile : playerPool) {
                UUID uuid = profile.getId();
                String name = profile.getName();
                SearchableDropdownWidget.DropdownIcon icon = (gfx, x, y, size) -> PlayerHeadRenderer.renderHead(gfx, name, uuid, x, y, size);
                playerEntries.add(new SearchableDropdownWidget.DropdownEntry<>(uuid, Component.literal(name), icon));
            }

            playerSearchDropdown = new SearchableDropdownWidget<>(
                    0, 0, searchWidth, 18,
                    Component.empty(), playerEntries, uuid -> {}
            );
            playerSearchDropdown.setSelectionRenderer(new SearchableDropdownWidget.SelectionRenderer<UUID>() {
                @Override
                public void render(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                SearchableDropdownWidget.DropdownEntry<UUID> selected, boolean isHovered) {
                    if (selected == null) {
                        graphics.drawString(font, Component.translatable("screen.logistics.computer.tab.protector.players.select").getString(), x + 5, y + (height - 8) / 2, 0xFF777777);
                        return;
                    }
                    if (selected.icon() != null) {
                        selected.icon().render(graphics, x + 4, y + (height - 10) / 2, 10);
                    }
                    graphics.drawString(font, selected.label(), x + 18, y + (height - 8) / 2, 0xFFFFFFFF);
                }
            });
            rightPanel.addChild(playerSearchDropdown, 0, currentY);

            addPlayerButton = new StyledButton(0, 0, 20, 18, Component.literal("+"), this::onAddPlayerClicked);
            addPlayerButton.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF);
            rightPanel.addChild(addPlayerButton, searchWidth + 4, currentY);
            currentY += 22 + 4;

            playerChips.clear();
            if (!currentRole.players.isEmpty()) {
                int chipWidth = Math.min(140, (innerWidth - 4) / 2);
                int chipHeight = 22;
                int col = 0;
                int rowY = currentY;
                int rowX = 0;

                for (int i = 0; i < currentRole.players.size(); i++) {
                    PlayerData player = currentRole.players.get(i);
                    if (col >= 2) {
                        col = 0;
                        rowX = 0;
                        rowY += chipHeight + 4;
                    }

                    PlayerChipWidget chip = new PlayerChipWidget(
                            0, 0, chipWidth, chipHeight,
                            player.id(), player.username(),
                            () -> onRemovePlayer(player.id())
                    );
                    rightPanel.addChild(chip, rowX, rowY);
                    playerChips.add(chip);

                    rowX += chipWidth + 4;
                    col++;
                }
                currentY = rowY + chipHeight + 12;
            } else {
                rightPanel.addChild(createLabel(Component.translatable("screen.logistics.computer.tab.protector.players.none_assigned").getString(), innerWidth, 0xFF777777), 0, currentY);
                currentY += 16 + 12;
            }
        } else {
            rightPanel.addChild(createLabel(
                    Component.translatable(isDefault ? "screen.logistics.computer.tab.protector.players.applies_to_all" : "screen.logistics.computer.tab.protector.role.select").getString(),
                    innerWidth, 0xFF777777), 0, currentY);
            currentY += 16 + 12;
        }

        rightPanel.addChild(createLabel(Component.translatable("screen.logistics.computer.tab.protector.players.flags").getString() + ":", innerWidth, 0xFFFFFFFF), 0, currentY);
        currentY += 12 + 4;

        flagWidgets.clear();
        if (currentRole != null && currentProtector != null) {
            int flagWidth = (innerWidth - 6) / 2;
            int col = 0;
            int rowX = 0;
            int rowY = currentY;

            for (FlagDefinition flagDef : FlagRegistry.forZone(currentProtector.creative)) {
                if (col >= 2) {
                    col = 0;
                    rowX = 0;
                    rowY += 58 + 6;
                }

                FlagData state = currentRole.flags.getOrDefault(flagDef.id(),
                        new FlagData(false, ActionType.DENY.name(), 0, ""));

                FlagWidget flagWidget = new FlagWidget(
                        0, 0, flagWidth, 58,
                        flagDef, state,
                        newState -> onFlagChanged(flagDef.id(), newState)
                );
                rightPanel.addChild(flagWidget, rowX, rowY);
                flagWidgets.add(flagWidget);

                rowX += flagWidth + 6;
                col++;
            }
            currentY = rowY + 58 + 12;
        }

        saveButton = new StyledButton(0, 0, 100, 20, Component.translatable("screen.logistics.generic.save_changes"), this::onSaveClicked);
        saveButton.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF);
        rightPanel.addChild(saveButton, innerWidth - 100, currentY);
    }

    private AbstractWidget createLabel(String text, int width, int color) {
        return new AbstractWidget(0, 0, width, 10, Component.literal(text)) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                graphics.drawString(Minecraft.getInstance().font, getMessage(), getX(), getY(), color, false);
            }
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
            @Override
            protected void updateWidgetNarration(NarrationElementOutput output) {}
        };
    }

    private void refreshRightPanel() {
        if (rightPanel != null) {
            int innerWidth = rightPanel.getWidth() - 12;
            rightPanel.clearChildren();
            buildRightPanel(innerWidth);
        }
    }

    private void refreshLeftPanel() {
        if (leftPanel != null) {
            int innerWidth = leftPanel.getWidth() - 12;
            leftPanel.clearChildren();
            buildLeftPanel(innerWidth);
        }
    }

    private EditableProtector getCurrentProtector() {
        if (selectedNodeIndex < 0 || selectedNodeIndex >= protectors.size()) return null;
        return protectors.get(selectedNodeIndex);
    }

    private List<EditableRole> getCurrentRoles() {
        EditableProtector protector = getCurrentProtector();
        if (protector == null) return List.of();
        return protector.roles;
    }

    private EditableRole getSelectedRole() {
        List<EditableRole> roles = getCurrentRoles();
        if (selectedRoleIndex >= 0 && selectedRoleIndex < roles.size()) {
            return roles.get(selectedRoleIndex);
        }
        return null;
    }

    private void onNodeSelected(Integer index) {
        if (index != null && index != selectedNodeIndex) {
            selectedNodeIndex = index;
            selectedRoleIndex = -1;
            refreshLeftPanel();
            refreshRightPanel();
        }
    }

    private void onRoleSelected(int index) {
        if (selectedRoleIndex != index) {
            selectedRoleIndex = index;
            for (int i = 0; i < roleEntries.size(); i++) {
                roleEntries.get(i).setSelected(i == index);
            }
            refreshRightPanel();
        }
    }

    private void onRoleNameChanged(int index, String newName) {
        List<EditableRole> roles = getCurrentRoles();
        if (index >= 0 && index < roles.size()) {
            roles.get(index).name = newName;
            markDirty();
        }
    }

    private void onRoleMoveUp(int index) {
        List<EditableRole> roles = getCurrentRoles();
        if (index > 0 && index < roles.size()) {
            if (roles.get(index).type == RoleData.RoleType.DEFAULT) return;
            if (roles.get(index - 1).type == RoleData.RoleType.DEFAULT) return;
            Collections.swap(roles, index, index - 1);
            for (int i = 0; i < roles.size(); i++) roles.get(i).order = i;
            markDirty();
            selectedRoleIndex = index - 1;
            refreshLeftPanel();
            refreshRightPanel();
        }
    }

    private void onRoleMoveDown(int index) {
        List<EditableRole> roles = getCurrentRoles();
        if (index >= 0 && index < roles.size() - 1) {
            if (roles.get(index).type == RoleData.RoleType.DEFAULT) return;
            if (roles.get(index + 1).type == RoleData.RoleType.DEFAULT) return;
            Collections.swap(roles, index, index + 1);
            for (int i = 0; i < roles.size(); i++) roles.get(i).order = i;
            markDirty();
            selectedRoleIndex = index + 1;
            refreshLeftPanel();
            refreshRightPanel();
        }
    }

    private void onAddRoleClicked() {
        closeAllDropdowns();
        addRolePopup.show();
    }

    private void onRoleCreated(String name) {
        EditableProtector protector = getCurrentProtector();
        if (protector == null) return;

        Map<String, FlagData> defaultFlags = new HashMap<>();
        for (FlagDefinition def : FlagRegistry.forZone(protector.creative)) {
            defaultFlags.put(def.id(), new FlagData(false, ActionType.DENY.name(), 0, ""));
        }

        RoleData data = new RoleData(
                UUID.randomUUID(),
                name,
                0, // will be reassigned below
                "logistics:textures/item/parcel.png",
                RoleData.RoleType.CUSTOM,
                List.of(),
                defaultFlags
        );

        // Insert at the very top (index 0) so the user can move it DOWN among custom roles
        protector.roles.add(0, new EditableRole(data));

        // Reassign orders so they match list index (DEFAULT keeps its own value)
        for (int i = 0; i < protector.roles.size(); i++) {
            EditableRole r = protector.roles.get(i);
            if (r.type != RoleData.RoleType.DEFAULT) {
                r.order = i;
            }
        }

        markDirty();
        selectedRoleIndex = 0;
        refreshLeftPanel();
        refreshRightPanel();
    }

    private void onDeleteRole(int index) {
        pendingDeleteIndex = index;
        confirmDeletePopup.show();
    }

    private void doConfirmDelete() {
        if (pendingDeleteIndex < 0) return;
        EditableProtector protector = getCurrentProtector();
        if (protector == null) return;

        List<EditableRole> roles = protector.roles;
        if (pendingDeleteIndex >= 0 && pendingDeleteIndex < roles.size()) {
            roles.remove(pendingDeleteIndex);

            // Reassign orders after removal
            for (int i = 0; i < roles.size(); i++) {
                EditableRole r = roles.get(i);
                if (r.type != RoleData.RoleType.DEFAULT) {
                    r.order = i;
                }
            }

            if (selectedRoleIndex == pendingDeleteIndex) {
                selectedRoleIndex = -1;
            } else if (selectedRoleIndex > pendingDeleteIndex) {
                selectedRoleIndex--;
            }
            markDirty();
            refreshLeftPanel();
            refreshRightPanel();
        }
        pendingDeleteIndex = -1;
    }

    private void onRadiusChanged(double newRadius) {
        EditableProtector protector = getCurrentProtector();
        if (protector == null) return;
        hasUnsavedChanges = true;
        protector.radius = (int) newRadius;
    }

    private void onAddPlayerClicked() {
        UUID selectedPlayer = playerSearchDropdown.getSelectedValue();
        if (selectedPlayer == null) return;

        EditableRole role = getSelectedRole();
        if (role == null || role.type == RoleData.RoleType.DEFAULT) return;

        for (PlayerData p : role.players) {
            if (p.id().equals(selectedPlayer)) return;
        }

        String username = null;
        for (GameProfile profile : playerPool) {
            if (profile.getId().equals(selectedPlayer)) {
                username = profile.getName();
                break;
            }
        }
        if (username != null) {
            role.players.add(new PlayerData(selectedPlayer, username));
            markDirty();
            refreshRightPanel();
        }
    }

    private void onRemovePlayer(UUID playerId) {
        EditableRole role = getSelectedRole();
        if (role == null || role.type == RoleData.RoleType.DEFAULT) return;
        role.players.removeIf(p -> p.id().equals(playerId));
        markDirty();
        refreshRightPanel();
    }

    private void onFlagChanged(String flagId, FlagData newState) {
        EditableRole role = getSelectedRole();
        if (role != null) {
            role.flags.put(flagId, newState);
            markDirty();
        }
    }

    private void onSaveClicked() {
        if (computerPos == null) return;
        Map<UUID, ProtectorData> data = new HashMap<>();
        for (EditableProtector ep : protectors) {
            ProtectionZone zone = ep.toZone();
            data.put(zone.nodeId(), new ProtectorData(zone.radius(), zone.creative(), zone.roles(), zone.powered()));
        }
        ClientNetworking.sendToServer(new ProtectionSavePacket(computerPos, data));
        hasUnsavedChanges = false;
    }

    private void markDirty() {
        hasUnsavedChanges = true;
    }

    private void onSaveAndClose() {
        onSaveClicked();
        if (parent != null) {
            parent.onClose();
        }
    }

    private void onDiscardAndClose() {
        hasUnsavedChanges = false;
        if (parent != null) {
            parent.onClose();
        }
    }

    private void closeAllDropdowns() {
        closeDropdownsInPanel(leftPanel);
        closeDropdownsInPanel(rightPanel);
        for (FlagWidget flag : flagWidgets) {
            flag.closeDropdown();
        }
    }

    private void closeDropdownsInPanel(ScrollablePanel panel) {
        if (panel == null) return;
        for (AbstractWidget child : panel.getChildren()) {
            if (child instanceof SearchableDropdownWidget<?> dropdown) {
                dropdown.closeMenu();
            }
        }
    }

    @Override
    public void tick() {
        if (leftPanel != null) leftPanel.tick();
        if (rightPanel != null) rightPanel.tick();
        for (FlagWidget flag : flagWidgets) {
            flag.tick();
        }
        if (addRolePopup != null) addRolePopup.tick();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta, int x, int y, int width, int height) {
        if (leftPanel != null) leftPanel.render(gfx, mouseX, mouseY, delta);
        if (rightPanel != null) rightPanel.render(gfx, mouseX, mouseY, delta);

        for (FlagWidget flag : flagWidgets) {
            flag.renderDropdownOverlay(gfx, mouseX, mouseY, delta);
        }

        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 1000);
        if (confirmDeletePopup != null && confirmDeletePopup.isActive()) {
            confirmDeletePopup.render(gfx, mouseX, mouseY, delta);
        }
        if (addRolePopup != null && addRolePopup.isActive()) {
            addRolePopup.render(gfx, mouseX, mouseY, delta);
        }
        if (unsavedPopup != null && unsavedPopup.isActive()) {
            unsavedPopup.render(gfx, mouseX, mouseY, delta);
        }
        gfx.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (confirmDeletePopup != null && confirmDeletePopup.isActive()) {
            return confirmDeletePopup.mouseClicked(mouseX, mouseY, button);
        }

        if (unsavedPopup != null && unsavedPopup.isActive()) {
            return unsavedPopup.mouseClicked(mouseX, mouseY, button);
        }
        if (addRolePopup != null && addRolePopup.isActive()) {
            return addRolePopup.mouseClicked(mouseX, mouseY, button);
        }

        if (nodeSelector != null && nodeSelector.isExpanded() && nodeSelector.isMouseOver(mouseX, mouseY)) {
            if (leftPanel != null && leftPanel.mouseClicked(mouseX, mouseY, button)) return true;
        }
        if (playerSearchDropdown != null && playerSearchDropdown.isExpanded() && playerSearchDropdown.isMouseOver(mouseX, mouseY)) {
            if (rightPanel != null && rightPanel.mouseClicked(mouseX, mouseY, button)) return true;
        }
        for (FlagWidget flag : flagWidgets) {
            if (flag != null && flag.isDropdownExpanded() && flag.isMouseOver(mouseX, mouseY)) {
                if (rightPanel != null && rightPanel.mouseClicked(mouseX, mouseY, button)) return true;
            }
        }

        if (rightPanel != null && rightPanel.isMouseOver(mouseX, mouseY)) {
            if (rightPanel.mouseClicked(mouseX, mouseY, button)) return true;
        }
        if (leftPanel != null && leftPanel.isMouseOver(mouseX, mouseY)) {
            if (leftPanel.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (unsavedPopup != null && unsavedPopup.isActive()) return true;
        if (addRolePopup != null && addRolePopup.isActive()) return true;

        if (rightPanel != null && rightPanel.mouseReleased(mouseX, mouseY, button)) return true;
        if (leftPanel != null && leftPanel.mouseReleased(mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (unsavedPopup != null && unsavedPopup.isActive()) return true;
        if (addRolePopup != null && addRolePopup.isActive()) return true;

        if (nodeSelector != null && nodeSelector.isExpanded() && nodeSelector.isMouseOver(mouseX, mouseY)) {
            if (leftPanel != null && leftPanel.mouseScrolled(mouseX, mouseY, delta)) return true;
        }
        if (playerSearchDropdown != null && playerSearchDropdown.isExpanded() && playerSearchDropdown.isMouseOver(mouseX, mouseY)) {
            if (rightPanel != null && rightPanel.mouseScrolled(mouseX, mouseY, delta)) return true;
        }
        for (FlagWidget flag : flagWidgets) {
            if (flag != null && flag.isDropdownExpanded() && flag.isMouseOver(mouseX, mouseY)) {
                if (rightPanel != null && rightPanel.mouseScrolled(mouseX, mouseY, delta)) return true;
            }
        }

        if (rightPanel != null && rightPanel.isMouseOver(mouseX, mouseY)) {
            if (rightPanel.mouseScrolled(mouseX, mouseY, delta)) return true;
        }
        if (leftPanel != null && leftPanel.isMouseOver(mouseX, mouseY)) {
            if (leftPanel.mouseScrolled(mouseX, mouseY, delta)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (unsavedPopup != null && unsavedPopup.isActive()) {
            return unsavedPopup.keyPressed(keyCode, scanCode, modifiers);
        }
        if (addRolePopup != null && addRolePopup.isActive()) {
            return addRolePopup.keyPressed(keyCode, scanCode, modifiers);
        }
        if (confirmDeletePopup != null && confirmDeletePopup.isActive()) {
            return confirmDeletePopup.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && hasUnsavedChanges) {
            closeAllDropdowns();
            unsavedPopup.show();
            return true;
        }

        if (rightPanel != null && rightPanel.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (leftPanel != null && leftPanel.keyPressed(keyCode, scanCode, modifiers)) return true;
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (unsavedPopup != null && unsavedPopup.isActive()) return false;
        if (addRolePopup != null && addRolePopup.isActive()) {
            return addRolePopup.charTyped(codePoint, modifiers);
        }

        if (rightPanel != null && rightPanel.charTyped(codePoint, modifiers)) return true;
        if (leftPanel != null && leftPanel.charTyped(codePoint, modifiers)) return true;
        return false;
    }

    @Override
    public boolean onAttemptClose() {
        if (hasUnsavedChanges) {
            closeAllDropdowns();
            unsavedPopup.show();
            return true;
        }
        return false;
    }
}
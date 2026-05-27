package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.blocks.computer.screen.ProtectionTabDummyData.*;
import com.restonic4.logistics.screens.tabs.Tab;
import com.restonic4.logistics.screens.widgets.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class ProtectionTab extends Tab {

    // UI State (preserved across init() calls)
    private int selectedNodeIndex = 0;
    private int selectedRoleIndex = -1;
    private boolean hasUnsavedChanges = false;

    // Popups
    private AddRolePopup addRolePopup;
    private UnsavedChangesPopup unsavedPopup;

    // Panels
    private ScrollablePanel leftPanel;
    private ScrollablePanel rightPanel;

    // Left panel widgets
    private SearchableDropdownWidget<ProtectionNode> nodeSelector;
    private final List<RoleEntryWidget> roleEntries = new ArrayList<>();
    private StyledButton addRoleButton;

    // Right panel widgets
    private EditBox roleNameField;
    private SearchableDropdownWidget<UUID> playerSearchDropdown;
    private StyledButton addPlayerButton;
    private final List<PlayerChipWidget> playerChips = new ArrayList<>();
    private final List<FlagWidget> flagWidgets = new ArrayList<>();
    private StyledButton saveButton;

    // Parent reference
    private Screen parent;
    private int contentX, contentY, contentWidth, contentHeight;

    public ProtectionTab() {
        super(Component.literal("Protection"));
    }

    @Override
    public void init(Screen parent, int x, int y, int width, int height) {
        this.parent = parent;
        this.contentX = x;
        this.contentY = y;
        this.contentWidth = width;
        this.contentHeight = height;

        int leftPanelWidth = Math.max(120, Math.min(160, (int) (width * 0.35)));
        int rightPanelWidth = width - leftPanelWidth - 6;
        int gap = 6;

        // Left panel
        leftPanel = new ScrollablePanel(x, y, leftPanelWidth, height);
        leftPanel.setPadding(6);

        // Right panel
        rightPanel = new ScrollablePanel(x + leftPanelWidth + gap, y, rightPanelWidth, height);
        rightPanel.setPadding(6);

        buildLeftPanel(leftPanelWidth - 12);
        buildRightPanel(rightPanelWidth - 12);

        // Popups
        if (addRolePopup == null) {
            addRolePopup = new AddRolePopup(parent, this::onRoleCreated, () -> {});
        }
        if (unsavedPopup == null) {
            unsavedPopup = new UnsavedChangesPopup(parent, this::onSaveAndClose, this::onDiscardAndClose, () -> {});
        }
    }

    private void buildLeftPanel(int innerWidth) {
        int currentY = 0;

        // Node selector
        List<SearchableDropdownWidget.DropdownEntry<ProtectionNode>> nodeOptions = new ArrayList<>();
        for (ProtectionNode node : ProtectionTabDummyData.NODES) {
            SearchableDropdownWidget.DropdownIcon icon = SearchableDropdownWidget.DropdownIcon.of(Blocks.BEACON);
            nodeOptions.add(new SearchableDropdownWidget.DropdownEntry<>(node, Component.literal(node.name), icon));
        }

        nodeSelector = new SearchableDropdownWidget<>(
                0, 0, innerWidth, 20,
                Component.empty(), nodeOptions, this::onNodeSelected
        );
        if (selectedNodeIndex >= 0 && selectedNodeIndex < ProtectionTabDummyData.NODES.size()) {
            nodeSelector.setSelectedIndex(selectedNodeIndex);
        }
        leftPanel.addChild(nodeSelector, 0, currentY);
        currentY += 24 + 4;

        // Role list
        roleEntries.clear();
        List<Role> roles = getCurrentRoles();
        for (int i = 0; i < roles.size(); i++) {
            Role role = roles.get(i);
            int finalI = i;
            RoleEntryWidget entry = new RoleEntryWidget(
                    0, 0, innerWidth, 24, role.icon, role.name,
                    newName -> onRoleNameChanged(finalI, newName),
                    i > 0 ? () -> onRoleMoveUp(finalI) : null,
                    i < roles.size() - 1 ? () -> onRoleMoveDown(finalI) : null,
                    selected -> onRoleSelected(finalI)
            );
            entry.setSelected(i == selectedRoleIndex);
            leftPanel.addChild(entry, 0, currentY);
            roleEntries.add(entry);
            currentY += 24 + 4;
        }

        // Add role button
        addRoleButton = new StyledButton(0, 0, innerWidth, 20, Component.literal("+"), this::onAddRoleClicked);
        addRoleButton.withColors(0xFF161616, 0xFF2A2A2A, 0xFFFFFFFF);
        leftPanel.addChild(addRoleButton, 0, currentY);
    }

    private void buildRightPanel(int innerWidth) {
        innerWidth = Math.max(20, innerWidth - 6);
        int currentY = 0;

        // Players label
        rightPanel.addChild(createLabel("Players:", innerWidth, 0xFFFFFFFF), 0, currentY);
        currentY += 12;

        // Player search row
        int searchWidth = innerWidth - 24;
        List<SearchableDropdownWidget.DropdownEntry<UUID>> playerEntries = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : ProtectionTabDummyData.ALL_PLAYERS.entrySet()) {
            UUID uuid = entry.getKey();
            String name = entry.getValue();
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
                    graphics.drawString(font, "Select player...", x + 5, y + (height - 8) / 2, 0xFF777777);
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

        // Player chips grid
        playerChips.clear();
        Role currentRole = getSelectedRole();
        if (currentRole != null && !currentRole.players.isEmpty()) {
            int chipWidth = Math.min(140, (innerWidth - 4) / 2);
            int chipHeight = 22;
            int col = 0;
            int rowY = currentY;
            int rowX = 0;

            for (int i = 0; i < currentRole.players.size(); i++) {
                AssignedPlayer player = currentRole.players.get(i);
                if (col >= 2) {
                    col = 0;
                    rowX = 0;
                    rowY += chipHeight + 4;
                }

                PlayerChipWidget chip = new PlayerChipWidget(
                        0, 0, chipWidth, chipHeight,
                        player.id, player.username,
                        () -> onRemovePlayer(player.id)
                );
                rightPanel.addChild(chip, rowX, rowY);
                playerChips.add(chip);

                rowX += chipWidth + 4;
                col++;
            }
            currentY = rowY + chipHeight + 12;
        } else {
            rightPanel.addChild(createLabel("No players assigned", innerWidth, 0xFF777777), 0, currentY);
            currentY += 16 + 12;
        }

        // Protection Flags label
        rightPanel.addChild(createLabel("Protection Flags:", innerWidth, 0xFFFFFFFF), 0, currentY);
        currentY += 12 + 4;

        // Flags grid
        flagWidgets.clear();
        if (currentRole != null) {
            int flagWidth = (innerWidth - 6) / 2;
            int col = 0;
            int rowX = 0;
            int rowY = currentY;

            for (ProtectionFlag flagDef : ProtectionTabDummyData.FLAGS) {
                if (col >= 2) {
                    col = 0;
                    rowX = 0;
                    rowY += 58 + 6; // uniform flag height + gap
                }

                FlagState state = currentRole.flags.getOrDefault(flagDef.id, new FlagState(false, ActionType.DENY, 0, ""));
                // Uniform height so config widgets never overflow the box
                FlagWidget flagWidget = new FlagWidget(
                        0, 0, flagWidth, 58,
                        flagDef, state,
                        newState -> onFlagChanged(flagDef.id, newState)
                );
                rightPanel.addChild(flagWidget, rowX, rowY);
                flagWidgets.add(flagWidget);

                rowX += flagWidth + 6;
                col++;
            }
            currentY = rowY + 58 + 12;
        }

        // Save button
        saveButton = new StyledButton(0, 0, 100, 20, Component.literal("Save Changes"), this::onSaveClicked);
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

    private List<Role> getCurrentRoles() {
        if (selectedNodeIndex < 0 || selectedNodeIndex >= ProtectionTabDummyData.NODES.size()) {
            return new ArrayList<>();
        }
        ProtectionNode node = ProtectionTabDummyData.NODES.get(selectedNodeIndex);
        return ProtectionTabDummyData.getRolesForNode(node.id);
    }

    private Role getSelectedRole() {
        List<Role> roles = getCurrentRoles();
        if (selectedRoleIndex >= 0 && selectedRoleIndex < roles.size()) {
            return roles.get(selectedRoleIndex);
        }
        return null;
    }

    // === Event Handlers ===

    private void onNodeSelected(ProtectionNode node) {
        int index = ProtectionTabDummyData.NODES.indexOf(node);
        if (index != selectedNodeIndex) {
            selectedNodeIndex = index;
            selectedRoleIndex = -1;
            refreshLeftPanel();
            refreshRightPanel();
        }
    }

    private void onRoleSelected(int index) {
        if (selectedRoleIndex != index) {
            selectedRoleIndex = index;
            // Update selection visuals
            for (int i = 0; i < roleEntries.size(); i++) {
                roleEntries.get(i).setSelected(i == index);
            }
            refreshRightPanel();
        }
    }

    private void onRoleNameChanged(int index, String newName) {
        List<Role> roles = getCurrentRoles();
        if (index >= 0 && index < roles.size()) {
            roles.get(index).name = newName;
            markDirty();
            // Update left panel entry
            if (index < roleEntries.size()) {
                roleEntries.get(index).setName(newName);
            }
            // Update right panel field if this is the selected role
            if (index == selectedRoleIndex && roleNameField != null) {
                roleNameField.setValue(newName);
            }
        }
    }

    private void onRoleNameFieldChanged(String text) {
        Role role = getSelectedRole();
        if (role != null && !text.isBlank()) {
            role.name = text;
            markDirty();
            // Sync to left panel
            if (selectedRoleIndex >= 0 && selectedRoleIndex < roleEntries.size()) {
                roleEntries.get(selectedRoleIndex).setName(text);
            }
        }
    }

    private void onRoleMoveUp(int index) {
        List<Role> roles = getCurrentRoles();
        if (index > 0 && index < roles.size()) {
            Collections.swap(roles, index, index - 1);
            markDirty();
            selectedRoleIndex = index - 1;
            refreshLeftPanel();
            refreshRightPanel();
        }
    }

    private void onRoleMoveDown(int index) {
        List<Role> roles = getCurrentRoles();
        if (index >= 0 && index < roles.size() - 1) {
            Collections.swap(roles, index, index + 1);
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
        List<Role> roles = getCurrentRoles();
        if (roles != null) {
            // Create new role with default flags (all off/deny)
            Map<String, FlagState> defaultFlags = new HashMap<>();
            for (ProtectionFlag f : ProtectionTabDummyData.FLAGS) {
                defaultFlags.put(f.id, new FlagState(false, ActionType.DENY, 0, ""));
            }
            Role newRole = new Role(
                    UUID.randomUUID(),
                    name,
                    roles.size(),
                    new ResourceLocation("logistics", "textures/item/parcel.png"),
                    defaultFlags
            );
            roles.add(newRole);
            markDirty();
            selectedRoleIndex = roles.size() - 1;
            refreshLeftPanel();
            refreshRightPanel();
        }
    }

    private void onAddPlayerClicked() {
        UUID selectedPlayer = playerSearchDropdown.getSelectedValue();
        if (selectedPlayer == null) return;

        Role role = getSelectedRole();
        if (role == null) return;

        // Check for duplicates
        for (AssignedPlayer p : role.players) {
            if (p.id.equals(selectedPlayer)) return;
        }

        String username = ProtectionTabDummyData.ALL_PLAYERS.get(selectedPlayer);
        if (username != null) {
            role.players.add(new AssignedPlayer(selectedPlayer, username));
            markDirty();
            refreshRightPanel();
        }
    }

    private void onRemovePlayer(UUID playerId) {
        Role role = getSelectedRole();
        if (role == null) return;

        role.players.removeIf(p -> p.id.equals(playerId));
        markDirty();
        refreshRightPanel();
    }

    private void onFlagChanged(String flagId, FlagState newState) {
        Role role = getSelectedRole();
        if (role != null) {
            role.flags.put(flagId, newState);
            markDirty();
        }
    }

    private void onSaveClicked() {
        // Currently a no-op for dummy data
        hasUnsavedChanges = false;
    }

    private void markDirty() {
        hasUnsavedChanges = true;
    }

    // === Popup Handlers ===

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

    // === Tab Lifecycle ===

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

        // Draw flag dropdown menus that extend outside the right panel scissor
        for (FlagWidget flag : flagWidgets) {
            flag.renderDropdownOverlay(gfx, mouseX, mouseY, delta);
        }

        // Popups at high Z so they cover everything
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 1000);
        if (addRolePopup != null && addRolePopup.isActive()) {
            addRolePopup.render(gfx, mouseX, mouseY, delta);
        }
        if (unsavedPopup != null && unsavedPopup.isActive()) {
            unsavedPopup.render(gfx, mouseX, mouseY, delta);
        }
        gfx.pose().popPose();
    }

    // === Input Forwarding ===

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (unsavedPopup != null && unsavedPopup.isActive()) {
            return unsavedPopup.mouseClicked(mouseX, mouseY, button);
        }
        if (addRolePopup != null && addRolePopup.isActive()) {
            return addRolePopup.mouseClicked(mouseX, mouseY, button);
        }

        // 1. Global Overlay Pass: Give expanded floating menus first dibs, matching their rendering stack
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

        // 2. Normal Front-to-Back Base Hit Testing (Early returns block fall-through)
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

        // Global Overlay Pass for Scrolling
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

        // ESC guard for unsaved changes
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
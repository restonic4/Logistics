package com.restonic4.logistics.blocks.computer.screen.triggers;

import com.restonic4.logistics.blocks.computer.automation.TriggerSavePacket;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.Trigger;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerAction;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.ActionRegistry;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.ActionType;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.TriggerRegistry;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.TriggerType;
import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import com.restonic4.logistics.blocks.computer.screen.UnsavedChangesPopup;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.ActionEditors;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.EditorBuilder;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.TriggerEditors;
import com.restonic4.logistics.networking.ClientNetworking;
import com.restonic4.logistics.screens.tabs.Tab;
import com.restonic4.logistics.screens.widgets.ScrollablePanel;
import com.restonic4.logistics.screens.widgets.SearchableDropdownWidget;
import com.restonic4.logistics.screens.widgets.StyledButton;
import com.restonic4.logistics.screens.widgets.ToggleWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The computer's automation editor. Always available on an installed computer (the trigger
 * system is a computer feature, unlike the node-dependent tabs).
 * <p>
 * Layout, streamer.bot style: trigger list on the left, the selected trigger's
 * configuration — firing settings plus its ordered action sequence — on the right.
 * Edits happen on client-side clones and only reach the server via the Save button
 * ({@link TriggerSavePacket}); the replicated node state is never mutated directly.
 */
public class TriggersTab extends Tab {
    private static final int ROW_H = 18;
    private static final int GAP = 4;
    private static final int ENTRY_H = 26;

    private final List<Trigger> triggers = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean hasUnsavedChanges = false;

    private Screen parent;
    private ScrollablePanel leftPanel;
    private ScrollablePanel rightPanel;
    private UnsavedChangesPopup unsavedPopup;

    private final List<TriggerListEntryWidget> triggerEntries = new ArrayList<>();
    private final List<SearchableDropdownWidget<?>> leftDropdowns = new ArrayList<>();
    private final List<SearchableDropdownWidget<?>> rightDropdowns = new ArrayList<>();
    private final List<EditBox> editBoxes = new ArrayList<>();

    private SearchableDropdownWidget<TriggerType<?>> addTriggerDropdown;
    private SearchableDropdownWidget<ActionType<?>> addActionDropdown;

    public TriggersTab() {
        super(Component.translatable("screen.logistics.computer.tab.triggers.title"));
        reloadFromNode();
    }

    /** Clones the replicated trigger configuration so edits never touch synced state. */
    private void reloadFromNode() {
        triggers.clear();
        if (ComputerScreen.getComputerNode() == null) return;
        for (Trigger trigger : ComputerScreen.getComputerNode().getTriggerManager().getTriggers()) {
            triggers.add(Trigger.createFromTag(trigger.save()));
        }
    }

    @Override
    public void init(Screen parent, int x, int y, int width, int height) {
        this.parent = parent;

        int leftPanelWidth = Math.max(120, Math.min(160, (int) (width * 0.35)));
        int gap = 6;
        int rightPanelWidth = width - leftPanelWidth - gap;

        leftPanel = new ScrollablePanel(x, y, leftPanelWidth, height);
        leftPanel.setPadding(6);
        rightPanel = new ScrollablePanel(x + leftPanelWidth + gap, y, rightPanelWidth, height);
        rightPanel.setPadding(6);

        buildLeftPanel(leftPanelWidth - 12);
        buildRightPanel(rightPanelWidth - 18);

        if (unsavedPopup == null) {
            unsavedPopup = new UnsavedChangesPopup(parent, this::onSaveAndClose, this::onDiscardAndClose, () -> {});
        }
    }

    // =====================================================================
    // Left panel: trigger list + add row
    // =====================================================================

    private void buildLeftPanel(int innerWidth) {
        leftDropdowns.clear();
        triggerEntries.clear();
        int currentY = 0;

        if (triggers.isEmpty()) {
            leftPanel.addChild(EditorBuilder.createLabel(
                    Component.translatable("screen.logistics.computer.tab.triggers.empty").getString(),
                    innerWidth, 0xFF777777), 0, currentY);
            currentY += 14;
        }

        for (int i = 0; i < triggers.size(); i++) {
            Trigger trigger = triggers.get(i);
            final int idx = i;
            TriggerListEntryWidget entry = new TriggerListEntryWidget(
                    0, 0, innerWidth, ENTRY_H,
                    trigger.getType().getDisplayName().getString(),
                    summaryFor(trigger),
                    () -> onTriggerSelected(idx),
                    () -> onTriggerDeleted(idx)
            );
            entry.setSelected(i == selectedIndex);
            leftPanel.addChild(entry, 0, currentY);
            triggerEntries.add(entry);
            currentY += ENTRY_H + GAP;
        }

        currentY += 4;

        List<SearchableDropdownWidget.DropdownEntry<TriggerType<?>>> typeEntries = new ArrayList<>();
        for (TriggerType<?> type : TriggerRegistry.getAll()) {
            typeEntries.add(new SearchableDropdownWidget.DropdownEntry<>(type, type.getDisplayName(), null));
        }
        addTriggerDropdown = new SearchableDropdownWidget<>(0, 0, innerWidth - 24, ROW_H,
                Component.empty(), typeEntries, t -> {});
        leftPanel.addChild(addTriggerDropdown, 0, currentY);
        leftDropdowns.add(addTriggerDropdown);

        StyledButton addBtn = new StyledButton(0, 0, 20, ROW_H, Component.literal("+"), this::onAddTrigger);
        leftPanel.addChild(addBtn, innerWidth - 20, currentY);
    }

    /** The trigger's own summary (from its registered editor) plus a generic action count. */
    private String summaryFor(Trigger trigger) {
        String detail = TriggerEditors.summary(trigger);
        int actions = trigger.getActions().size();
        return detail + " • " + actions + (actions == 1 ? " action" : " actions");
    }

    private void onTriggerSelected(int index) {
        if (selectedIndex == index) return;
        selectedIndex = index;
        for (int i = 0; i < triggerEntries.size(); i++) {
            triggerEntries.get(i).setSelected(i == index);
        }
        refreshRightPanel();
    }

    private void onTriggerDeleted(int index) {
        if (index < 0 || index >= triggers.size()) return;
        triggers.remove(index);
        if (selectedIndex == index) selectedIndex = -1;
        else if (selectedIndex > index) selectedIndex--;
        markDirty();
        refreshLeftPanel();
        refreshRightPanel();
    }

    private void onAddTrigger() {
        TriggerType<?> type = addTriggerDropdown != null ? addTriggerDropdown.getSelectedValue() : null;
        if (type == null) return;
        triggers.add(type.create());
        selectedIndex = triggers.size() - 1;
        markDirty();
        refreshLeftPanel();
        refreshRightPanel();
    }

    // =====================================================================
    // Right panel: selected trigger configuration
    // =====================================================================

    private void buildRightPanel(int innerWidth) {
        rightDropdowns.clear();
        editBoxes.clear();
        addActionDropdown = null;

        Trigger trigger = getSelectedTrigger();
        if (trigger == null) {
            rightPanel.addChild(EditorBuilder.createLabel(
                    Component.translatable("screen.logistics.computer.tab.triggers.select_hint").getString(),
                    innerWidth, 0xFF777777), 0, 0);

            // Still offer Save: deleting the last trigger must be persistable too.
            StyledButton saveBtn = new StyledButton(0, 0, 100, 20,
                    Component.translatable("screen.logistics.generic.save_changes"), this::onSaveClicked);
            rightPanel.addChild(saveBtn, innerWidth - 100, 18);
            return;
        }

        int half = (innerWidth - GAP) / 2;
        int currentY = 0;

        // --- Firing settings ---
        rightPanel.addChild(EditorBuilder.createLabel(
                Component.translatable("screen.logistics.computer.tab.triggers.mode").getString(),
                half, 0xFFAAAAAA), 0, currentY);
        rightPanel.addChild(EditorBuilder.createLabel(
                Component.translatable("screen.logistics.computer.tab.triggers.overlap").getString(),
                half, 0xFFAAAAAA), half + GAP, currentY);
        currentY += 11;

        List<SearchableDropdownWidget.DropdownEntry<Trigger.ExecutionMode>> modeEntries = new ArrayList<>();
        for (Trigger.ExecutionMode mode : Trigger.ExecutionMode.values()) {
            modeEntries.add(new SearchableDropdownWidget.DropdownEntry<>(mode,
                    Component.translatable("screen.logistics.computer.tab.triggers.mode." + mode.name().toLowerCase()), null));
        }
        SearchableDropdownWidget<Trigger.ExecutionMode> modeDropdown = new SearchableDropdownWidget<>(
                0, 0, half, ROW_H, Component.empty(), modeEntries,
                mode -> { trigger.setMode(mode); markDirty(); });
        modeDropdown.setSelectedValueSilently(trigger.getMode());
        rightPanel.addChild(modeDropdown, 0, currentY);
        rightDropdowns.add(modeDropdown);

        ToggleWidget overlapToggle = new ToggleWidget(half + GAP, 0, 36, 14,
                trigger.allowsOverlap(), v -> { trigger.setAllowOverlap(v); markDirty(); });
        rightPanel.addChild(overlapToggle, half + GAP, currentY + 2);
        currentY += ROW_H + GAP * 2;

        // --- Type-specific settings (delegated to the trigger's registered editor) ---
        EditorBuilder typeBuilder = new EditorBuilder(rightPanel, innerWidth, currentY,
                rightDropdowns, editBoxes, this::markDirty);
        TriggerEditors.buildConfig(trigger, typeBuilder);
        currentY = typeBuilder.y();

        currentY += 4;

        // --- Actions ---
        rightPanel.addChild(EditorBuilder.createLabel(
                Component.translatable("screen.logistics.computer.tab.triggers.actions").getString(),
                innerWidth, 0xFFFFFFFF), 0, currentY);
        currentY += 14;

        List<TriggerAction> actions = trigger.getActions();
        for (int i = 0; i < actions.size(); i++) {
            currentY = buildActionEditor(trigger, actions.get(i), i, actions.size(), innerWidth, currentY);
        }

        // Add-action row
        List<SearchableDropdownWidget.DropdownEntry<ActionType<?>>> actionTypeEntries = new ArrayList<>();
        for (ActionType<?> type : ActionRegistry.getAll()) {
            actionTypeEntries.add(new SearchableDropdownWidget.DropdownEntry<>(type, type.getDisplayName(), null));
        }
        addActionDropdown = new SearchableDropdownWidget<>(0, 0, innerWidth - 24, ROW_H,
                Component.empty(), actionTypeEntries, t -> {});
        rightPanel.addChild(addActionDropdown, 0, currentY);
        rightDropdowns.add(addActionDropdown);

        StyledButton addActionBtn = new StyledButton(0, 0, 20, ROW_H, Component.literal("+"),
                () -> onAddAction(trigger));
        rightPanel.addChild(addActionBtn, innerWidth - 20, currentY);
        currentY += ROW_H + GAP * 3;

        // --- Save ---
        StyledButton saveBtn = new StyledButton(0, 0, 100, 20,
                Component.translatable("screen.logistics.generic.save_changes"), this::onSaveClicked);
        rightPanel.addChild(saveBtn, innerWidth - 100, currentY);
    }

    // =====================================================================
    // Action editors
    // =====================================================================

    private int buildActionEditor(Trigger trigger, TriggerAction action, int index, int total,
                                  int innerWidth, int currentY) {
        // Header bar: "N. <action name>" + move/delete controls
        rightPanel.addChild(EditorBuilder.createHeaderBar(
                (index + 1) + ". " + action.getType().getDisplayName().getString(),
                innerWidth), 0, currentY);

        int btn = 14;
        int btnY = currentY + 2;
        if (index > 0) {
            StyledButton up = new StyledButton(0, 0, btn, btn, Component.literal("↑"),
                    () -> onMoveAction(trigger, index, index - 1));
            rightPanel.addChild(up, innerWidth - btn * 3 - 6, btnY);
        }
        if (index < total - 1) {
            StyledButton down = new StyledButton(0, 0, btn, btn, Component.literal("↓"),
                    () -> onMoveAction(trigger, index, index + 1));
            rightPanel.addChild(down, innerWidth - btn * 2 - 3, btnY);
        }
        StyledButton delete = new StyledButton(0, 0, btn, btn, Component.literal("✕"),
                () -> onDeleteAction(trigger, index));
        delete.withColors(StyledButton.DEFAULT_BG, StyledButton.DEFAULT_BORDER, 0xFFFF5555);
        rightPanel.addChild(delete, innerWidth - btn, btnY);
        currentY += ROW_H + GAP;

        // Action-specific settings (delegated to the action's registered editor).
        EditorBuilder builder = new EditorBuilder(rightPanel, innerWidth, currentY,
                rightDropdowns, editBoxes, this::markDirty);
        ActionEditors.buildConfig(action, builder);
        currentY = builder.y();

        return currentY + GAP;
    }

    private void onAddAction(Trigger trigger) {
        ActionType<?> type = addActionDropdown != null ? addActionDropdown.getSelectedValue() : null;
        if (type == null) return;
        trigger.addAction(type.create());
        markDirty();
        refreshLeftPanel();
        refreshRightPanel();
    }

    private void onMoveAction(Trigger trigger, int from, int to) {
        List<TriggerAction> copy = new ArrayList<>(trigger.getActions());
        if (from < 0 || from >= copy.size() || to < 0 || to >= copy.size()) return;
        Collections.swap(copy, from, to);
        trigger.clearActions();
        copy.forEach(trigger::addAction);
        markDirty();
        refreshRightPanel();
    }

    private void onDeleteAction(Trigger trigger, int index) {
        List<TriggerAction> copy = new ArrayList<>(trigger.getActions());
        if (index < 0 || index >= copy.size()) return;
        copy.remove(index);
        trigger.clearActions();
        copy.forEach(trigger::addAction);
        markDirty();
        refreshLeftPanel();
        refreshRightPanel();
    }

    // =====================================================================
    // Plumbing
    // =====================================================================

    private Trigger getSelectedTrigger() {
        if (selectedIndex < 0 || selectedIndex >= triggers.size()) return null;
        return triggers.get(selectedIndex);
    }

    private void refreshLeftPanel() {
        if (leftPanel == null) return;
        int innerWidth = leftPanel.getWidth() - 12;
        leftPanel.clearChildren();
        buildLeftPanel(innerWidth);
    }

    private void refreshRightPanel() {
        if (rightPanel == null) return;
        int innerWidth = rightPanel.getWidth() - 18;
        rightPanel.clearChildren();
        buildRightPanel(innerWidth);
    }

    private void markDirty() {
        hasUnsavedChanges = true;
    }

    private void onSaveClicked() {
        if (ComputerScreen.getComputerNode() == null) return;
        ClientNetworking.sendToServer(new TriggerSavePacket(ComputerScreen.getComputerNode().getBlockPos(), triggers));
        hasUnsavedChanges = false;
        refreshLeftPanel();
    }

    private void onSaveAndClose() {
        onSaveClicked();
        if (parent != null) parent.onClose();
    }

    private void onDiscardAndClose() {
        hasUnsavedChanges = false;
        if (parent != null) parent.onClose();
    }

    private void closeAllDropdowns() {
        for (SearchableDropdownWidget<?> dropdown : leftDropdowns) dropdown.closeMenu();
        for (SearchableDropdownWidget<?> dropdown : rightDropdowns) dropdown.closeMenu();
    }

    // =====================================================================
    // Tab events
    // =====================================================================

    @Override
    public void tick() {
        if (leftPanel != null) leftPanel.tick();
        if (rightPanel != null) rightPanel.tick();
        for (EditBox box : editBoxes) box.tick();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta, int x, int y, int width, int height) {
        if (leftPanel != null) leftPanel.render(gfx, mouseX, mouseY, delta);
        if (rightPanel != null) rightPanel.render(gfx, mouseX, mouseY, delta);

        if (unsavedPopup != null && unsavedPopup.isActive()) {
            gfx.pose().pushPose();
            gfx.pose().translate(0, 0, 1000);
            unsavedPopup.render(gfx, mouseX, mouseY, delta);
            gfx.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (unsavedPopup != null && unsavedPopup.isActive()) {
            return unsavedPopup.mouseClicked(mouseX, mouseY, button);
        }

        // Expanded dropdown menus float over everything; route to their panel first.
        for (SearchableDropdownWidget<?> dropdown : leftDropdowns) {
            if (dropdown.isExpanded() && dropdown.isMouseOver(mouseX, mouseY)) {
                if (leftPanel != null && leftPanel.mouseClicked(mouseX, mouseY, button)) return true;
            }
        }
        for (SearchableDropdownWidget<?> dropdown : rightDropdowns) {
            if (dropdown.isExpanded() && dropdown.isMouseOver(mouseX, mouseY)) {
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

        if (rightPanel != null && rightPanel.mouseReleased(mouseX, mouseY, button)) return true;
        if (leftPanel != null && leftPanel.mouseReleased(mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (unsavedPopup != null && unsavedPopup.isActive()) return true;

        for (SearchableDropdownWidget<?> dropdown : leftDropdowns) {
            if (dropdown.isExpanded() && dropdown.isMouseOver(mouseX, mouseY)) {
                if (leftPanel != null && leftPanel.mouseScrolled(mouseX, mouseY, delta)) return true;
            }
        }
        for (SearchableDropdownWidget<?> dropdown : rightDropdowns) {
            if (dropdown.isExpanded() && dropdown.isMouseOver(mouseX, mouseY)) {
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

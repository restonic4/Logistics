package com.restonic4.logistics.blocks.computer.screen.triggers.editors;

import com.restonic4.logistics.blocks.audio_station.AudioStationNode;
import com.restonic4.logistics.blocks.base.NameIdentifier;
import com.restonic4.logistics.blocks.computer.automation.triggers.util.AudioStationTarget;
import com.restonic4.logistics.blocks.computer.automation.triggers.util.NetworkNodeTarget;
import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import com.restonic4.logistics.blocks.network_switch.NetworkSwitchNode;
import com.restonic4.logistics.blocks.redstone_reader.RedstoneReaderNode;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.client.ClientNetworkManager;
import com.restonic4.logistics.screens.widgets.NumberPickerWidget;
import com.restonic4.logistics.screens.widgets.ScrollablePanel;
import com.restonic4.logistics.screens.widgets.SearchableDropdownWidget;
import com.restonic4.logistics.screens.widgets.ToggleWidget;
import com.restonic4.logistics.screens.widgets.SearchableDropdownWidget.DropdownEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The toolbox every {@link TriggerEditor}/{@link ActionEditor} draws with. It hides the
 * panel, the running Y cursor, the two-column grid math, and the widget registration
 * bookkeeping so an editor only has to declare its rows ("a labeled dropdown here, a
 * number picker there"). It also owns the station/sound dropdown construction so the
 * editors never reach into {@link ComputerScreen} themselves.
 * <p>
 * One builder is created per config block (see {@code TriggersTab}); the caller seeds it
 * with the starting Y and reads {@link #y()} back afterwards to continue laying out.
 */
public class EditorBuilder {
    /** Dropdown sentinel meaning "all stations on the network". */
    public static final UUID ALL_STATIONS = new UUID(0L, 0L);
    /** Dropdown sentinel meaning "all nodes of this kind on the network". */
    public static final UUID ALL_NODES = new UUID(0L, 0L);

    private static final int ROW_H = 18;
    private static final int GAP = 4;

    private final ScrollablePanel panel;
    private final int half;
    /** Widgets that float (dropdown menus / edit boxes) must be registered so the owning tab can route input to them. */
    private final List<SearchableDropdownWidget<?>> dropdownSink;
    private final List<EditBox> editBoxSink;
    private final Runnable markDirty;
    private int y;

    public EditorBuilder(ScrollablePanel panel, int innerWidth, int startY,
                         List<SearchableDropdownWidget<?>> dropdownSink,
                         List<EditBox> editBoxSink, Runnable markDirty) {
        this.panel = panel;
        this.half = (innerWidth - GAP) / 2;
        this.dropdownSink = dropdownSink;
        this.editBoxSink = editBoxSink;
        this.markDirty = markDirty;
        this.y = startY;
    }

    // =====================================================================
    // Layout cursor
    // =====================================================================

    /** The Y position the cursor has reached; read back by the caller to keep laying out. */
    public int y() { return y; }

    /** Width of one of the two columns. */
    public int half() { return half; }

    /** Advances past a row of standard-height controls. Call once after placing a row. */
    public void endRow() { y += ROW_H + GAP; }

    /** Marks the edited configuration dirty (drives the unsaved-changes prompt). */
    public void markDirty() { markDirty.run(); }

    private int columnX(int col) { return col == 0 ? 0 : half + GAP; }

    // =====================================================================
    // Labels
    // =====================================================================

    /** A single column-0 caption above the next row. */
    public void label(Component text) {
        panel.addChild(createLabel(text.getString(), half, 0xFFAAAAAA), 0, y);
        y += 11;
    }

    /** Captions for both columns above the next row. */
    public void columnLabels(Component left, Component right) {
        panel.addChild(createLabel(left.getString(), half, 0xFFAAAAAA), 0, y);
        panel.addChild(createLabel(right.getString(), half, 0xFFAAAAAA), half + GAP, y);
        y += 11;
    }

    // =====================================================================
    // Controls (placed at the current cursor; call endRow() when the row is full)
    // =====================================================================

    /** A dropdown over an arbitrary value set in the given column (0 = left, 1 = right). */
    public <E> SearchableDropdownWidget<E> dropdown(int col, List<DropdownEntry<E>> entries,
                                                    E selected, Consumer<E> onSelect) {
        SearchableDropdownWidget<E> dropdown = new SearchableDropdownWidget<>(
                0, 0, half, ROW_H, Component.empty(), entries,
                value -> { onSelect.accept(value); markDirty.run(); });
        dropdown.setSelectedValueSilently(selected);
        panel.addChild(dropdown, columnX(col), y);
        dropdownSink.add(dropdown);
        return dropdown;
    }

    /** A dropdown over an enum's constants, each labeled via {@code labeler}. */
    public <E extends Enum<E>> SearchableDropdownWidget<E> enumDropdown(int col, E[] values, E selected,
                                                                       Function<E, Component> labeler,
                                                                       Consumer<E> onSelect) {
        List<DropdownEntry<E>> entries = new ArrayList<>();
        for (E value : values) {
            entries.add(new DropdownEntry<>(value, labeler.apply(value), null));
        }
        return dropdown(col, entries, selected, onSelect);
    }

    /** A numeric spinner clamped to {@code [min, max]} with {@code decimals} places. */
    public NumberPickerWidget number(int col, double value, double min, double max, int decimals,
                                     Consumer<Double> onChange) {
        NumberPickerWidget picker = new NumberPickerWidget(0, 0, half, ROW_H, Component.empty(), value,
                v -> { onChange.accept(v); markDirty.run(); });
        picker.setRange(min, max);
        picker.setDecimalPlaces(decimals);
        panel.addChild(picker, columnX(col), y);
        return picker;
    }

    /** An on/off switch. */
    public ToggleWidget toggle(int col, boolean value, Consumer<Boolean> onChange) {
        ToggleWidget toggle = new ToggleWidget(0, 0, 36, 14, value,
                v -> { onChange.accept(v); markDirty.run(); });
        panel.addChild(toggle, columnX(col), y + 2);
        return toggle;
    }

    /** A single-line text field. */
    public EditBox editBox(int col, String value, int maxLength, Consumer<String> onChange) {
        // Inset by 1px: a bordered EditBox paints its frame outside its own bounds.
        // Inside a ScrollablePanel nothing focuses widgets for us, so do it on click.
        EditBox box = new EditBox(Minecraft.getInstance().font, 0, 0, half - 2, ROW_H - 2, Component.empty()) {
            @Override
            public boolean mouseClicked(double mx, double my, int btn) {
                if (this.clicked(mx, my)) this.setFocused(true);
                return super.mouseClicked(mx, my, btn);
            }
        };
        box.setMaxLength(maxLength);
        box.setValue(value == null ? "" : value);
        box.setTextColor(0xFFFFFFFF);
        box.setResponder(text -> { onChange.accept(text); markDirty.run(); });
        panel.addChild(box, columnX(col) + 1, y + 1);
        editBoxSink.add(box);
        return box;
    }

    /** A station picker bound to {@code target}, including the "all stations" sentinel. */
    public SearchableDropdownWidget<UUID> stationDropdown(int col, AudioStationTarget target) {
        List<DropdownEntry<UUID>> entries = new ArrayList<>();
        entries.add(new DropdownEntry<>(ALL_STATIONS,
                Component.translatable("screen.logistics.computer.tab.triggers.all_stations"), null));
        for (AudioStationNode station : stations()) {
            entries.add(new DropdownEntry<>(station.getUUID(), Component.literal(station.getSafeName()), null));
        }

        SearchableDropdownWidget<UUID> dropdown = new SearchableDropdownWidget<>(
                0, 0, half, ROW_H, Component.empty(), entries,
                uuid -> {
                    if (uuid == null || ALL_STATIONS.equals(uuid)) target.setAllStations();
                    else target.setStation(uuid);
                    markDirty.run();
                });
        dropdown.setSelectedValueSilently(target.isAllStations() ? ALL_STATIONS : target.getStationId());
        panel.addChild(dropdown, columnX(col), y);
        dropdownSink.add(dropdown);
        return dropdown;
    }

    /** A sound picker; the empty value means "each station's own configured audio". */
    public SearchableDropdownWidget<String> soundDropdown(int col, String selected, Consumer<String> onSelect) {
        List<DropdownEntry<String>> entries = new ArrayList<>();
        entries.add(new DropdownEntry<>("",
                Component.translatable("screen.logistics.computer.tab.triggers.sound.station_default"), null));
        for (String sound : ClientNetworkManager.getUploadedSounds()) {
            entries.add(new DropdownEntry<>(sound,
                    Component.literal(ClientNetworkManager.getSoundDisplayName(sound)), null));
        }

        SearchableDropdownWidget<String> dropdown = new SearchableDropdownWidget<>(
                0, 0, half, ROW_H, Component.empty(), entries,
                value -> { onSelect.accept(value); markDirty.run(); });
        dropdown.setSelectedValueSilently(selected == null ? "" : selected);
        panel.addChild(dropdown, columnX(col), y);
        dropdownSink.add(dropdown);
        return dropdown;
    }

    /**
     * A picker over a list of named network nodes bound to {@code target}, including the "all" sentinel.
     * Works for any {@link NetworkNode} that is also a {@link NameIdentifier} (redstone readers, switches).
     */
    public <T extends NetworkNode & NameIdentifier> SearchableDropdownWidget<UUID> nodeDropdown(
            int col, NetworkNodeTarget<T> target, List<T> nodes, Component allLabel) {
        List<DropdownEntry<UUID>> entries = new ArrayList<>();
        entries.add(new DropdownEntry<>(ALL_NODES, allLabel, null));
        for (T node : nodes) {
            entries.add(new DropdownEntry<>(node.getUUID(), Component.literal(node.getSafeName()), null));
        }

        SearchableDropdownWidget<UUID> dropdown = new SearchableDropdownWidget<>(
                0, 0, half, ROW_H, Component.empty(), entries,
                uuid -> {
                    if (uuid == null || ALL_NODES.equals(uuid)) target.setAll();
                    else target.setNode(uuid);
                    markDirty.run();
                });
        dropdown.setSelectedValueSilently(target.isAll() ? ALL_NODES : target.getNodeId());
        panel.addChild(dropdown, columnX(col), y);
        dropdownSink.add(dropdown);
        return dropdown;
    }

    // =====================================================================
    // Station resolution + shared widget factories (also used for summaries)
    // =====================================================================

    /** The audio stations on the computer's network, or empty if there is no network. */
    public static List<AudioStationNode> stations() {
        if (ComputerScreen.getEnergyNetwork() == null) return List.of();
        return ComputerScreen.getEnergyNetwork().getAudioStations();
    }

    /** The display name for a station target: "all stations", the station's name, or "?" if gone. */
    public static String stationLabel(AudioStationTarget target) {
        if (target.isAllStations()) {
            return Component.translatable("screen.logistics.computer.tab.triggers.all_stations").getString();
        }
        for (AudioStationNode station : stations()) {
            if (station.getUUID().equals(target.getStationId())) return station.getSafeName();
        }
        return "?";
    }

    /** The redstone readers on the computer's network, or empty if there is no network. */
    public static List<RedstoneReaderNode> redstoneReaders() {
        if (ComputerScreen.getEnergyNetwork() == null) return List.of();
        return ComputerScreen.getEnergyNetwork().getRedstoneReaders();
    }

    /** The network switches on the computer's network, or empty if there is no network. */
    public static List<NetworkSwitchNode> networkSwitches() {
        if (ComputerScreen.getEnergyNetwork() == null) return List.of();
        return ComputerScreen.getEnergyNetwork().getNetworkSwitches();
    }

    /** The display name for a node target: {@code allLabel}, the node's name, or "?" if it is gone. */
    public static <T extends NetworkNode & NameIdentifier> String nodeLabel(
            NetworkNodeTarget<T> target, List<T> nodes, String allLabel) {
        if (target.isAll()) return allLabel;
        for (T node : nodes) {
            if (node.getUUID().equals(target.getNodeId())) return node.getSafeName();
        }
        return "?";
    }

    /** A plain, non-interactive text label. */
    public static AbstractWidget createLabel(String text, int width, int color) {
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

    /** A filled header bar with a caption, used as an action's title strip. */
    public static AbstractWidget createHeaderBar(String text, int width) {
        return new AbstractWidget(0, 0, width, ROW_H, Component.literal(text)) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF161616);
                graphics.drawString(Minecraft.getInstance().font, getMessage(),
                        getX() + 4, getY() + (getHeight() - 8) / 2, 0xFFFFFFFF, false);
            }
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
            @Override
            protected void updateWidgetNarration(NarrationElementOutput output) {}
        };
    }
}

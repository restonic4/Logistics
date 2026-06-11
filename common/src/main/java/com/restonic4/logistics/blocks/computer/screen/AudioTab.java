package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.audio_station.AudioDeletePacket;
import com.restonic4.logistics.blocks.audio_station.AudioStationConfigPacket;
import com.restonic4.logistics.blocks.audio_station.AudioStationNode;
import com.restonic4.logistics.blocks.audio_station.AudioUploadPacket;
import com.restonic4.logistics.networking.ClientNetworking;
import com.restonic4.logistics.networks.client.ClientNetworkManager;
import com.restonic4.logistics.screens.tabs.Tab;
import com.restonic4.logistics.screens.widgets.NumberPickerWidget;
import com.restonic4.logistics.screens.widgets.SearchableDropdownWidget;
import com.restonic4.logistics.screens.widgets.StyledButton;
import com.restonic4.logistics.screens.widgets.ToggleWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AudioTab extends Tab {
    private static final int CHUNK_SIZE = 30000; // Under 32767 limit with header overhead

    private SearchableDropdownWidget<AudioStationNode> stationSelector;
    private SearchableDropdownWidget<String> soundSelector;
    private NumberPickerWidget radiusPicker;
    private NumberPickerWidget volumePicker;
    private NumberPickerWidget pitchPicker;
    private ToggleWidget loopToggle;
    private ToggleWidget redstoneToggle;
    private StyledButton applyButton;
    private StyledButton deleteButton;
    private EditBox pathField;

    private final List<AudioStationNode> audioStations;
    private final List<String> sounds;

    // Persisted state
    private AudioStationNode savedStation = null;
    private String savedSound = null;
    private double savedRadius = 32;
    private double savedVolume = 1.0;
    private double savedPitch = 1.0;
    private boolean savedLoop = false;
    private boolean savedRedstone = false;

    public AudioTab() {
        super(Component.translatable("screen.logistics.computer.tab.audio.title"));
        this.audioStations = ComputerScreen.getEnergyNetwork().getAudioStations();
        this.sounds = ClientNetworkManager.getUploadedSounds();
    }

    @Override
    public void init(Screen parent, int x, int y, int width, int height) {
        Font font = Minecraft.getInstance().font;
        int midX = x + width / 2;

        // === LEFT COLUMN: Station & Sound Selection ===
        int leftX = x + 10;
        int colY = y + 10;

        // Station selector
        List<SearchableDropdownWidget.DropdownEntry<AudioStationNode>> stationEntries = new ArrayList<>();
        for (AudioStationNode s : audioStations) {
            stationEntries.add(new SearchableDropdownWidget.DropdownEntry<>(s, Component.literal(s.getSafeName()), SearchableDropdownWidget.DropdownIcon.of(BlockRegistry.AUDIO_STATION_BLOCK.getBlock())));
        }
        stationSelector = new SearchableDropdownWidget<>(leftX, colY, 160, 18,
                Component.empty(), stationEntries, this::onStationSelected);
        if (savedStation != null) stationSelector.setSelectedValue(savedStation);
        parent.addRenderableWidget(stationSelector);
        colY += 28;

        // Sound selector
        List<SearchableDropdownWidget.DropdownEntry<String>> soundEntries = buildSoundEntries();
        soundSelector = new SearchableDropdownWidget<>(leftX, colY, 160, 18,
                Component.empty(), soundEntries, s -> {
            savedSound = s;
            updateDeleteButton();
        });
        if (savedSound != null) soundSelector.setSelectedValue(savedSound);
        parent.addRenderableWidget(soundSelector);
        colY += 32;

        // === CONFIG ROW ===
        int configY = colY;
        radiusPicker = new NumberPickerWidget(leftX, configY, 70, 18, Component.empty(), savedRadius, v -> {});
        radiusPicker.setRange(0, 320);
        radiusPicker.setDecimalPlaces(0);
        parent.addRenderableWidget(radiusPicker);

        volumePicker = new NumberPickerWidget(leftX + 80, configY, 70, 18, Component.empty(), savedVolume, v -> {});
        volumePicker.setRange(0, 5);
        volumePicker.setStep(0.1);
        volumePicker.setDecimalPlaces(2);
        parent.addRenderableWidget(volumePicker);

        pitchPicker = new NumberPickerWidget(leftX + 160, configY, 70, 18, Component.empty(), savedPitch, v -> {});
        pitchPicker.setStep(0.1);
        pitchPicker.setDecimalPlaces(2);
        parent.addRenderableWidget(pitchPicker);
        colY += 28;

        // === TOGGLES ROW ===
        int toggleY = colY;
        loopToggle = new ToggleWidget(leftX, toggleY, 36, 14, savedLoop, v -> {});
        parent.addRenderableWidget(loopToggle);

        redstoneToggle = new ToggleWidget(leftX + 90, toggleY, 36, 14, savedRedstone, v -> {});
        parent.addRenderableWidget(redstoneToggle);
        colY += 28;

        // Apply button
        applyButton = new StyledButton(leftX, colY, 100, 20,
                Component.translatable("screen.logistics.generic.apply"), this::applyConfig);
        parent.addRenderableWidget(applyButton);

        // === RIGHT COLUMN: Library Management ===
        int rightX = midX + 10;
        int rightY = y + 10;

        // Path input
        pathField = new EditBox(font, rightX, rightY, 180, 18, Component.empty());
        pathField.setMaxLength(512);
        pathField.setValue("");
        pathField.setTextColor(0xFFFFFFFF);
        pathField.setBordered(true);
        parent.addRenderableWidget(pathField);
        rightY += 24;

        // Upload button
        StyledButton uploadBtn = new StyledButton(rightX, rightY, 180, 20,
                Component.translatable("screen.logistics.computer.tab.audio.upload"),
                this::uploadFromPath);
        parent.addRenderableWidget(uploadBtn);
        rightY += 28;

        // Delete button
        deleteButton = new StyledButton(rightX, rightY, 180, 20,
                Component.translatable("screen.logistics.computer.tab.audio.delete"), this::deleteSelected);
        deleteButton.active = false;
        parent.addRenderableWidget(deleteButton);

        refreshFromSelection();
    }

    private List<SearchableDropdownWidget.DropdownEntry<String>> buildSoundEntries() {
        List<SearchableDropdownWidget.DropdownEntry<String>> list = new ArrayList<>();
        list.add(new SearchableDropdownWidget.DropdownEntry<>("",
                Component.translatable("screen.logistics.computer.tab.audio.none"), null));
        for (String s : sounds) {
            list.add(new SearchableDropdownWidget.DropdownEntry<>(s,
                    Component.literal(s), SearchableDropdownWidget.DropdownIcon.of(Items.NOTE_BLOCK)));
        }
        return list;
    }

    public void refreshData() {
        if (stationSelector != null) {
            List<SearchableDropdownWidget.DropdownEntry<AudioStationNode>> entries = new ArrayList<>();
            for (AudioStationNode s : audioStations) {
                entries.add(new SearchableDropdownWidget.DropdownEntry<>(s,
                        Component.literal(s.getSafeName()), SearchableDropdownWidget.DropdownIcon.of(BlockRegistry.AUDIO_STATION_BLOCK.getBlock())));
            }
            stationSelector.setOptions(entries);
            if (savedStation != null) stationSelector.setSelectedValue(savedStation);
        }
        if (soundSelector != null) {
            soundSelector.setOptions(buildSoundEntries());
            if (savedSound != null) soundSelector.setSelectedValue(savedSound);
        }
        updateDeleteButton();
    }

    private void refreshFromSelection() {
        AudioStationNode s = stationSelector != null ? stationSelector.getSelectedValue() : null;
        if (s == null) return;
        radiusPicker.setValue(s.getRadius());
        volumePicker.setValue(s.getVolume());
        pitchPicker.setValue(s.getPitch());
        loopToggle.setValue(s.isLooping());
        redstoneToggle.setValue(s.isRedstoneMode());
        if (s.getAudioPath() != null && !s.getAudioPath().isEmpty()) {
            soundSelector.setSelectedValue(s.getAudioPath());
        }
    }

    private void onStationSelected(AudioStationNode s) {
        savedStation = s;
        refreshFromSelection();
    }

    private void updateDeleteButton() {
        String sound = soundSelector != null ? soundSelector.getSelectedValue() : null;
        if (sound == null || sound.isEmpty() || deleteButton == null) {
            if (deleteButton != null) deleteButton.active = false;
            return;
        }
        String[] parts = sound.split("/", 2);
        boolean owner = parts.length == 2 &&
                parts[0].equals(Minecraft.getInstance().player.getUUID().toString());
        deleteButton.active = owner;
    }

    private void uploadFromPath() {
        if (pathField == null) return;
        String path = pathField.getValue();
        if (path == null || path.isBlank()) return;
        Path p = Path.of(path.trim());
        if (!p.toString().toLowerCase().endsWith(".wav")) return;
        try {
            byte[] data = Files.readAllBytes(p);
            if (data.length > 1024 * 1024 * 15) return; // 10MB max total
            String filename = p.getFileName().toString();

            // Send in chunks
            int totalChunks = (data.length + CHUNK_SIZE - 1) / CHUNK_SIZE;
            for (int i = 0; i < totalChunks; i++) {
                int start = i * CHUNK_SIZE;
                int end = Math.min(start + CHUNK_SIZE, data.length);
                byte[] chunk = new byte[end - start];
                System.arraycopy(data, start, chunk, 0, chunk.length);
                ClientNetworking.sendToServer(new AudioUploadPacket(filename, chunk, i, totalChunks));
            }
            pathField.setValue("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteSelected() {
        String sound = soundSelector.getSelectedValue();
        if (sound == null || sound.isEmpty()) return;
        ClientNetworking.sendToServer(new AudioDeletePacket(sound));
    }

    private void applyConfig() {
        AudioStationNode s = stationSelector.getSelectedValue();
        if (s == null) return;
        String sound = soundSelector.getSelectedValue();
        if (sound == null) sound = "";

        ClientNetworking.sendToServer(new AudioStationConfigPacket(
                s.getBlockPos(), sound,
                (float) volumePicker.getValue(),
                (float) pitchPicker.getValue(),
                (float) radiusPicker.getValue(),
                loopToggle.getValue(), redstoneToggle.getValue()
        ));

        savedSound = sound;
        savedRadius = radiusPicker.getValue();
        savedVolume = volumePicker.getValue();
        savedPitch = pitchPicker.getValue();
        savedLoop = loopToggle.getValue();
        savedRedstone = redstoneToggle.getValue();
    }

    @Override
    public void tick() {
        if (radiusPicker != null) radiusPicker.tick();
        if (volumePicker != null) volumePicker.tick();
        if (pitchPicker != null) pitchPicker.tick();
        if (pathField != null) pathField.tick();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta, int x, int y, int width, int height) {
        Font font = Minecraft.getInstance().font;
        int leftX = x + 10;
        int rightX = x + width / 2 + 10;

        // Left column labels
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.station").getString(), leftX, y + 2, 0xFFAAAAAA, false);
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.sound").getString(), leftX, y + 30, 0xFFAAAAAA, false);
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.radius").getString(), leftX, y + 64, 0xFFAAAAAA, false);
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.volume").getString(), leftX + 80, y + 64, 0xFFAAAAAA, false);
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.pitch").getString(), leftX + 160, y + 64, 0xFFAAAAAA, false);
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.loop").getString(), leftX + 40, y + 92, 0xFFAAAAAA, false);
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.redstone").getString(), leftX + 130, y + 92, 0xFFAAAAAA, false);

        // Right column labels
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.library").getString(), rightX, y + 2, 0xFFAAAAAA, false);
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.paste_path").getString(), rightX, y + 66, 0xFF777777, false);

        gfx.drawString(font, "Stations: " + audioStations.size(), x + 10, y + 160, 0xFFFF0000, false);
        gfx.drawString(font, "Sounds: " + sounds.size(), x + 10, y + 172, 0xFFFF0000, false);
    }

    @Override
    public void onHide() {
        if (stationSelector != null) savedStation = stationSelector.getSelectedValue();
        if (soundSelector != null) savedSound = soundSelector.getSelectedValue();
        if (radiusPicker != null) savedRadius = radiusPicker.getValue();
        if (volumePicker != null) savedVolume = volumePicker.getValue();
        if (pitchPicker != null) savedPitch = pitchPicker.getValue();
        if (loopToggle != null) savedLoop = loopToggle.getValue();
        if (redstoneToggle != null) savedRedstone = redstoneToggle.getValue();
    }
}
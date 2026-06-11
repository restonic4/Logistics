package com.restonic4.logistics.blocks.computer.screen;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.audio_station.AudioDeletePacket;
import com.restonic4.logistics.blocks.audio_station.AudioStationConfigPacket;
import com.restonic4.logistics.blocks.audio_station.AudioStationControlPacket;
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
import java.util.UUID;

/**
 * Audio station management: per-station configuration (sound, radius, volume, pitch, loop,
 * auto play) with manual Play/Stop controls on the left, and the shared sound library
 * (upload/delete) plus a network-wide stop on the right.
 * <p>
 * For automated playback (alarms, playlists, synced stations) see the Triggers tab —
 * stations meant to be trigger-driven should have auto play disabled here.
 */
public class AudioTab extends Tab {
    private static final int CHUNK_SIZE = 30000; // Under the 32767 packet limit with header overhead
    private static final long MAX_UPLOAD_BYTES = 1024L * 1024L * 15L;

    private static final int LABEL_COLOR = 0xFFAAAAAA;
    private static final int HINT_COLOR = 0xFF777777;

    private SearchableDropdownWidget<UUID> stationSelector;
    private SearchableDropdownWidget<String> soundSelector;
    private NumberPickerWidget radiusPicker;
    private NumberPickerWidget volumePicker;
    private NumberPickerWidget pitchPicker;
    private ToggleWidget loopToggle;
    private ToggleWidget autoPlayToggle;
    private StyledButton deleteButton;
    private EditBox pathField;

    private List<String> sounds = new ArrayList<>();

    // Selection survives tab switches
    private UUID selectedStationId = null;

    public AudioTab() {
        super(Component.translatable("screen.logistics.computer.tab.audio.title"));
    }

    private List<AudioStationNode> getStations() {
        if (ComputerScreen.getEnergyNetwork() == null) return List.of();
        return ComputerScreen.getEnergyNetwork().getAudioStations();
    }

    /** Resolves the selected station against the live replicated network state. */
    private AudioStationNode getSelectedStation() {
        if (selectedStationId == null) return null;
        for (AudioStationNode station : getStations()) {
            if (station.getUUID().equals(selectedStationId)) return station;
        }
        return null;
    }

    @Override
    public void init(Screen parent, int x, int y, int width, int height) {
        Font font = Minecraft.getInstance().font;
        this.sounds = ClientNetworkManager.getUploadedSounds();

        int midX = x + width / 2;
        int leftX = x + 10;
        int leftW = midX - leftX - 10;
        int rightX = midX + 10;
        int rightW = x + width - rightX - 10;

        // === LEFT COLUMN: station configuration & control ===
        int colY = y + 12;

        List<AudioStationNode> stations = getStations();
        if (selectedStationId == null && !stations.isEmpty()) {
            selectedStationId = stations.get(0).getUUID();
        }

        List<SearchableDropdownWidget.DropdownEntry<UUID>> stationEntries = new ArrayList<>();
        for (AudioStationNode station : stations) {
            stationEntries.add(new SearchableDropdownWidget.DropdownEntry<>(station.getUUID(),
                    Component.literal(station.getSafeName()),
                    SearchableDropdownWidget.DropdownIcon.of(BlockRegistry.AUDIO_STATION_BLOCK.getBlock())));
        }
        stationSelector = new SearchableDropdownWidget<>(leftX, colY, leftW, 18,
                Component.empty(), stationEntries, this::onStationSelected);
        if (selectedStationId != null) stationSelector.setSelectedValueSilently(selectedStationId);
        parent.addRenderableWidget(stationSelector);
        colY += 18 + 16;

        List<SearchableDropdownWidget.DropdownEntry<String>> soundEntries = buildSoundEntries();
        soundSelector = new SearchableDropdownWidget<>(leftX, colY, leftW, 18,
                Component.empty(), soundEntries, s -> updateDeleteButton());
        parent.addRenderableWidget(soundSelector);
        colY += 18 + 16;

        int pickerW = (leftW - 8) / 3;
        radiusPicker = new NumberPickerWidget(leftX, colY, pickerW, 18, Component.empty(), 32, v -> {});
        radiusPicker.setRange(0, 320);
        radiusPicker.setDecimalPlaces(0);
        parent.addRenderableWidget(radiusPicker);

        volumePicker = new NumberPickerWidget(leftX + pickerW + 4, colY, pickerW, 18, Component.empty(), 1.0, v -> {});
        volumePicker.setRange(0, 5);
        volumePicker.setStep(0.1);
        volumePicker.setDecimalPlaces(2);
        parent.addRenderableWidget(volumePicker);

        pitchPicker = new NumberPickerWidget(leftX + (pickerW + 4) * 2, colY, pickerW, 18, Component.empty(), 1.0, v -> {});
        pitchPicker.setRange(0.5, 2);
        pitchPicker.setStep(0.1);
        pitchPicker.setDecimalPlaces(2);
        parent.addRenderableWidget(pitchPicker);
        colY += 18 + 6;

        loopToggle = new ToggleWidget(leftX, colY, 32, 14, false, v -> {});
        parent.addRenderableWidget(loopToggle);
        autoPlayToggle = new ToggleWidget(leftX + leftW / 2, colY, 32, 14, true, v -> {});
        parent.addRenderableWidget(autoPlayToggle);
        colY += 14 + 8;

        StyledButton applyButton = new StyledButton(leftX, colY, leftW, 18,
                Component.translatable("screen.logistics.generic.apply"), this::applyConfig);
        parent.addRenderableWidget(applyButton);
        colY += 18 + 4;

        int halfBtn = (leftW - 4) / 2;
        StyledButton playButton = new StyledButton(leftX, colY, halfBtn, 18,
                Component.translatable("screen.logistics.computer.tab.audio.play"),
                () -> sendControl(AudioStationControlPacket.Action.PLAY));
        parent.addRenderableWidget(playButton);

        StyledButton stopButton = new StyledButton(leftX + halfBtn + 4, colY, halfBtn, 18,
                Component.translatable("screen.logistics.computer.tab.audio.stop"),
                () -> sendControl(AudioStationControlPacket.Action.STOP));
        parent.addRenderableWidget(stopButton);

        // === RIGHT COLUMN: sound library & network-wide control ===
        int rightY = y + 12;

        pathField = new EditBox(font, rightX, rightY, rightW, 18, Component.empty());
        pathField.setMaxLength(512);
        pathField.setTextColor(0xFFFFFFFF);
        parent.addRenderableWidget(pathField);
        rightY += 18 + 14;

        StyledButton uploadBtn = new StyledButton(rightX, rightY, rightW, 18,
                Component.translatable("screen.logistics.computer.tab.audio.upload"), this::uploadFromPath);
        parent.addRenderableWidget(uploadBtn);
        rightY += 18 + 4;

        deleteButton = new StyledButton(rightX, rightY, rightW, 18,
                Component.translatable("screen.logistics.computer.tab.audio.delete"), this::deleteSelected);
        deleteButton.active = false;
        parent.addRenderableWidget(deleteButton);
        rightY += 18 + 14;

        StyledButton stopAllBtn = new StyledButton(rightX, rightY, rightW, 18,
                Component.translatable("screen.logistics.computer.tab.audio.stop_all"),
                () -> {
                    if (ComputerScreen.getComputerNode() != null) {
                        ClientNetworking.sendToServer(new AudioStationControlPacket(
                                ComputerScreen.getComputerNode().getBlockPos(),
                                AudioStationControlPacket.Action.STOP_ALL));
                    }
                });
        stopAllBtn.withColors(StyledButton.DEFAULT_BG, StyledButton.DEFAULT_BORDER, 0xFFFF7777);
        parent.addRenderableWidget(stopAllBtn);

        refreshFromSelection();
        updateDeleteButton();
    }

    private List<SearchableDropdownWidget.DropdownEntry<String>> buildSoundEntries() {
        List<SearchableDropdownWidget.DropdownEntry<String>> list = new ArrayList<>();
        list.add(new SearchableDropdownWidget.DropdownEntry<>("",
                Component.translatable("screen.logistics.computer.tab.audio.none"), null));
        for (String s : sounds) {
            list.add(new SearchableDropdownWidget.DropdownEntry<>(s,
                    Component.literal(ClientNetworkManager.getSoundDisplayName(s)),
                    SearchableDropdownWidget.DropdownIcon.of(Items.NOTE_BLOCK)));
        }
        return list;
    }

    /** Loads the selected station's replicated config into the editor widgets. */
    private void refreshFromSelection() {
        AudioStationNode station = getSelectedStation();
        if (station == null) return;
        radiusPicker.setValue(station.getRadius());
        volumePicker.setValue(station.getVolume());
        pitchPicker.setValue(station.getPitch());
        loopToggle.setValue(station.isLooping());
        autoPlayToggle.setValue(station.isAutoPlay());
        soundSelector.setSelectedValueSilently(station.getAudioPath() != null ? station.getAudioPath() : "");
        updateDeleteButton();
    }

    private void onStationSelected(UUID stationId) {
        selectedStationId = stationId;
        refreshFromSelection();
    }

    private void updateDeleteButton() {
        if (deleteButton == null) return;
        String sound = soundSelector != null ? soundSelector.getSelectedValue() : null;
        if (sound == null || sound.isEmpty()) {
            deleteButton.active = false;
            return;
        }
        String[] parts = sound.split("/", 2);
        deleteButton.active = parts.length == 2
                && parts[0].equals(Minecraft.getInstance().player.getUUID().toString());
    }

    private void uploadFromPath() {
        if (pathField == null) return;
        String path = pathField.getValue();
        if (path == null || path.isBlank()) return;
        Path p = Path.of(path.trim());
        if (!p.toString().toLowerCase().endsWith(".wav")) return;
        try {
            byte[] data = Files.readAllBytes(p);
            if (data.length > MAX_UPLOAD_BYTES) return;
            String filename = p.getFileName().toString();

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
        AudioStationNode station = getSelectedStation();
        if (station == null) return;
        String sound = soundSelector.getSelectedValue();
        if (sound == null) sound = "";

        ClientNetworking.sendToServer(new AudioStationConfigPacket(
                station.getBlockPos(), sound,
                (float) volumePicker.getValue(),
                (float) pitchPicker.getValue(),
                (float) radiusPicker.getValue(),
                loopToggle.getValue(), autoPlayToggle.getValue()
        ));
    }

    private void sendControl(AudioStationControlPacket.Action action) {
        AudioStationNode station = getSelectedStation();
        if (station == null) return;
        ClientNetworking.sendToServer(new AudioStationControlPacket(station.getBlockPos(), action));
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
        int midX = x + width / 2;
        int leftX = x + 10;
        int leftW = midX - leftX - 10;
        int rightX = midX + 10;

        // Column separator
        gfx.fill(midX, y + 6, midX + 1, y + height - 6, 0xFF2A2A2A);

        // Left column labels
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.station").getString(),
                leftX, y + 3, LABEL_COLOR, false);
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.sound").getString(),
                leftX, y + 37, LABEL_COLOR, false);
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.radius").getString(),
                leftX, y + 71, LABEL_COLOR, false);
        int pickerW = (leftW - 8) / 3;
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.volume").getString(),
                leftX + pickerW + 4, y + 71, LABEL_COLOR, false);
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.pitch").getString(),
                leftX + (pickerW + 4) * 2, y + 71, LABEL_COLOR, false);

        // Toggle labels (toggles sit at y + 106)
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.loop").getString(),
                leftX + 36, y + 109, LABEL_COLOR, false);
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.autoplay").getString(),
                leftX + leftW / 2 + 36, y + 109, LABEL_COLOR, false);

        // Live station status
        AudioStationNode station = getSelectedStation();
        if (station != null) {
            boolean playing = station.isPlaying();
            String status = Component.translatable(playing
                    ? "screen.logistics.computer.tab.audio.status.playing"
                    : "screen.logistics.computer.tab.audio.status.idle").getString();
            gfx.drawString(font, status, leftX + leftW - font.width(status), y + 3,
                    playing ? 0xFF55FF55 : 0xFF777777, false);
        }

        // Right column labels
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.library").getString(),
                rightX, y + 3, LABEL_COLOR, false);
        gfx.drawString(font, Component.translatable("screen.logistics.computer.tab.audio.paste_path").getString(),
                rightX, y + 33, HINT_COLOR, false);
    }
}

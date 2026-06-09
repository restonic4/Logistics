package com.restonic4.logistics.blocks.audio_station;

import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class AudioStationSyncPacket implements S2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio_station/sync");

    private final List<AudioStationNode.AudioStationData> stations;
    private final List<String> availableSounds;

    public AudioStationSyncPacket(List<AudioStationNode.AudioStationData> stations, List<String> availableSounds) {
        this.stations = stations;
        this.availableSounds = availableSounds;
    }

    public AudioStationSyncPacket(FriendlyByteBuf buf) {
        this.stations = new ArrayList<>();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            stations.add(new AudioStationNode.AudioStationData(
                    buf.readBlockPos(),
                    buf.readUtf(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readBoolean()
            ));
        }
        this.availableSounds = new ArrayList<>();
        int sc = buf.readInt();
        for (int i = 0; i < sc; i++) availableSounds.add(buf.readUtf());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(stations.size());
        for (AudioStationNode.AudioStationData s : stations) {
            buf.writeBlockPos(s.pos());
            buf.writeUtf(s.audioPath());
            buf.writeFloat(s.volume());
            buf.writeFloat(s.pitch());
            buf.writeFloat(s.radius());
            buf.writeBoolean(s.looping());
            buf.writeBoolean(s.redstoneMode());
        }
        buf.writeInt(availableSounds.size());
        for (String s : availableSounds) buf.writeUtf(s);
    }

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> ComputerScreen.setAudioStations(stations, availableSounds));
    }
}
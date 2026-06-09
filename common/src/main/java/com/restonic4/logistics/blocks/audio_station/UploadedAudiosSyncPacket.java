package com.restonic4.logistics.blocks.audio_station;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import com.restonic4.logistics.networking.S2CPacket;
import com.restonic4.logistics.networks.client.ClientNetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class UploadedAudiosSyncPacket implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("audio_station/sync");

    private final List<String> availableSounds;

    public UploadedAudiosSyncPacket(List<String> availableSounds) {
        this.availableSounds = availableSounds;
    }

    public UploadedAudiosSyncPacket(FriendlyByteBuf buf) {
        this.availableSounds = new ArrayList<>();
        int sc = buf.readInt();
        for (int i = 0; i < sc; i++) availableSounds.add(buf.readUtf());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(availableSounds.size());
        for (String s : availableSounds) buf.writeUtf(s);
    }

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> ClientNetworkManager.setUploadedSounds(availableSounds));
    }
}
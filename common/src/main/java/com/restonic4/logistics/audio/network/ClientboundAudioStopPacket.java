package com.restonic4.logistics.audio.network;

import com.restonic4.logistics.audio.client.ClientAudioManager;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class ClientboundAudioStopPacket implements S2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio_stop");

    private final UUID sourceId;

    public ClientboundAudioStopPacket(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public ClientboundAudioStopPacket(FriendlyByteBuf buf) {
        this.sourceId = buf.readUUID();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(sourceId);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> ClientAudioManager.getInstance().handleStop(this));
    }

    public UUID getSourceId() { return sourceId; }
}
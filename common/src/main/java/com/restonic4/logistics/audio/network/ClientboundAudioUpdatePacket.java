package com.restonic4.logistics.audio.network;

import com.restonic4.logistics.audio.client.ClientAudioManager;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class ClientboundAudioUpdatePacket implements S2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio_update");

    private final UUID sourceId;
    private final Vec3 position;
    private final float volume;
    private final float pitch;
    private final float radius;

    public ClientboundAudioUpdatePacket(UUID sourceId, Vec3 position, float volume, float pitch, float radius) {
        this.sourceId = sourceId;
        this.position = position;
        this.volume = volume;
        this.pitch = pitch;
        this.radius = radius;
    }

    public ClientboundAudioUpdatePacket(FriendlyByteBuf buf) {
        this.sourceId = buf.readUUID();
        this.position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
        this.radius = buf.readFloat();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(sourceId);
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
        buf.writeFloat(radius);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> ClientAudioManager.getInstance().handleUpdate(this));
    }

    public UUID getSourceId() { return sourceId; }
    public Vec3 getPosition() { return position; }
    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }
    public float getRadius() { return radius; }
}
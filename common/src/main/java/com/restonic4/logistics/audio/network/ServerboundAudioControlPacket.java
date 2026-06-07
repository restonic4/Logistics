package com.restonic4.logistics.audio.network;

import com.restonic4.logistics.audio.AudioFormat;
import com.restonic4.logistics.audio.server.ServerAudioManager;
import com.restonic4.logistics.networking.C2SPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class ServerboundAudioControlPacket implements C2SPacket {
    public static final ResourceLocation ID = new ResourceLocation("logistics", "audio_control");

    public enum Action { PLAY, STOP, UPDATE }

    private final Action action;
    private final UUID targetId;
    private final Vec3 position;
    private final ResourceLocation soundId;
    private final String url;
    private final AudioFormat format;
    private final boolean isRadio;
    private final float volume;
    private final float pitch;
    private final float radius;

    public ServerboundAudioControlPacket(Action action, UUID targetId, Vec3 position, ResourceLocation soundId,
                                         String url, AudioFormat format, boolean isRadio,
                                         float volume, float pitch, float radius) {
        this.action = action;
        this.targetId = targetId;
        this.position = position;
        this.soundId = soundId;
        this.url = url;
        this.format = format;
        this.isRadio = isRadio;
        this.volume = volume;
        this.pitch = pitch;
        this.radius = radius;
    }

    public ServerboundAudioControlPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.targetId = buf.readBoolean() ? buf.readUUID() : null;
        this.position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.isRadio = buf.readBoolean();
        this.url = buf.readUtf(32767);
        this.soundId = buf.readBoolean() ? buf.readResourceLocation() : null;
        this.format = buf.readEnum(AudioFormat.class);
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
        this.radius = buf.readFloat();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeBoolean(targetId != null);
        if (targetId != null) buf.writeUUID(targetId);
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
        buf.writeBoolean(isRadio);
        buf.writeUtf(url);
        buf.writeBoolean(soundId != null);
        if (soundId != null) buf.writeResourceLocation(soundId);
        buf.writeEnum(format);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
        buf.writeFloat(radius);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            switch (action) {
                case PLAY -> {
                    if (isRadio) {
                        ServerAudioManager.getInstance().playRadio(player.serverLevel(), position, url, format, volume, pitch, radius);
                    } else {
                        ServerAudioManager.getInstance().playSound(player.serverLevel(), position, soundId, format, volume, pitch, radius);
                    }
                }
                case STOP -> {
                    if (targetId != null) ServerAudioManager.getInstance().stop(targetId);
                }
                case UPDATE -> {
                    if (targetId != null) {
                        ServerAudioManager.getInstance().setVolume(targetId, volume);
                        ServerAudioManager.getInstance().setPitch(targetId, pitch);
                        ServerAudioManager.getInstance().setRadius(targetId, radius);
                    }
                }
            }
        });
    }
}
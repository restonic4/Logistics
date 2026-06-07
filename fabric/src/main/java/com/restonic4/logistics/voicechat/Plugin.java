package com.restonic4.logistics.voicechat;

import com.restonic4.logistics.Constants;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class Plugin implements VoicechatPlugin {
    private VoicechatServerApi api;
    private LocationalAudioChannel walkieChannel;

    @Override
    public String getPluginId() {
        return Constants.MOD_ID + ":walkie_talkie";
    }

    @Override
    public void initialize(VoicechatApi api) {
        this.api = (VoicechatServerApi) api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        registration.registerEvent(VoicechatServerStartedEvent.class, e -> walkieChannel = null);
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null) return;

        // Get the actual ServerPlayer from the API wrapper
        Object entity = event.getSenderConnection().getPlayer().getEntity();
        if (!(entity instanceof ServerPlayer player)) return;

        if (!WalkieTalkieManager.isTransmitting(player.getUUID())) {
            return; // Not using walkie-talkie; let normal proximity chat handle it
        }

        // Cancel the normal proximity packet so it doesn't also play nearby
        /*if (event.isCancellable()) {
            event.cancel();
        }*/

        // Lazily create the locational channel at 0, 64, 0 in the overworld
        if (walkieChannel == null || walkieChannel.isClosed()) {
            ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
            if (overworld == null) return;

            walkieChannel = api.createLocationalAudioChannel(
                    UUID.randomUUID(),
                    api.fromServerLevel(overworld),
                    api.createPosition(0, 64, 0)
            );

            if (walkieChannel != null) {
                walkieChannel.setDistance(64); // Audible range around the block
            }
        }

        if (walkieChannel == null) return;

        MicrophonePacket packet = event.getPacket();

        // 1) Stream to the hardcoded block location
        walkieChannel.send(packet);

        // 2) Echo back to the sender so they hear themselves
        // MicrophonePacket implements ConvertablePacket, which lets us turn it into
        // a StaticSoundPacket for direct playback on the sender's client.
        api.sendStaticSoundPacketTo(event.getSenderConnection(), packet.toStaticSoundPacket());
    }
}
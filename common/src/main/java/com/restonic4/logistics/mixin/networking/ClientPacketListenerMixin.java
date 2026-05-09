package com.restonic4.logistics.mixin.networking;

import com.restonic4.logistics.networking.NetworkingRegistry;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void onHandleCustomPayload(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
        Function<FriendlyByteBuf, S2CPacket> decoder = NetworkingRegistry.getS2CDecoder(packet.getIdentifier());
        if (decoder != null) {
            FriendlyByteBuf buf = packet.getData();
            FriendlyByteBuf copiedBuf = new FriendlyByteBuf(buf.copy());
            Minecraft mc = Minecraft.getInstance();

            mc.execute(() -> {
                try {
                    S2CPacket packetInstance = decoder.apply(copiedBuf);
                    packetInstance.handle(mc);
                } finally {
                    copiedBuf.release();
                }
            });
            ci.cancel();
        }
    }
}

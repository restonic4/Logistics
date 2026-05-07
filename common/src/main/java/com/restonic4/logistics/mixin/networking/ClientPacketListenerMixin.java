package com.restonic4.logistics.mixin.networking;

import com.restonic4.logistics.networking.ClientPacketHandler;
import com.restonic4.logistics.networking.NetworkingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void onHandleCustomPayload(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
        ResourceLocation id = packet.getIdentifier();
        ClientPacketHandler handler = NetworkingRegistry.getClientHandler(id);

        if (handler != null) {
            FriendlyByteBuf buf = packet.getData();
            FriendlyByteBuf copiedBuf = new FriendlyByteBuf(buf.copy());
            Minecraft mc = Minecraft.getInstance();

            mc.execute(() -> {
                try {
                    handler.handle(mc, copiedBuf);
                } finally {
                    copiedBuf.release();
                }
            });
            ci.cancel();
        }
    }
}

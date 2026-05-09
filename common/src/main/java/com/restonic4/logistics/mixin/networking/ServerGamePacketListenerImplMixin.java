package com.restonic4.logistics.mixin.networking;

import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networking.NetworkingRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void logistics$onHandleCustomPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        Function<FriendlyByteBuf, C2SPacket> decoder = NetworkingRegistry.getC2SDecoder(packet.getIdentifier());
        if (decoder != null) {
            FriendlyByteBuf buf = new FriendlyByteBuf(packet.getData().copy());
            C2SPacket packetInstance = decoder.apply(buf);

            this.player.server.execute(() -> {
                try {
                    packetInstance.handle(this.player.server, this.player);
                } finally {
                    buf.release();
                }
            });
            ci.cancel();
        }
    }
}

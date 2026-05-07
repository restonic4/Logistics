package com.restonic4.logistics.mixin.networking;

import com.restonic4.logistics.networking.NetworkingRegistry;
import com.restonic4.logistics.networking.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void logistics$onHandleCustomPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        ResourceLocation id = packet.getIdentifier();
        PacketHandler handler = NetworkingRegistry.getServerHandler(id);

        if (handler != null) {
            FriendlyByteBuf buf = packet.getData();
            FriendlyByteBuf copiedBuf = new FriendlyByteBuf(buf.copy());

            this.player.server.execute(() -> {
                try {
                    handler.handle(this.player.server, this.player, copiedBuf);
                } finally {
                    copiedBuf.release();
                }
            });
            ci.cancel();
        }
    }
}

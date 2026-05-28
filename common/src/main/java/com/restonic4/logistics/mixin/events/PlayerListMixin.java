package com.restonic4.logistics.mixin.events;

import com.restonic4.logistics.events.PlayerEvents;
import com.restonic4.logistics.events.core.EventResult;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Shadow @Final private MinecraftServer server;

    @Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void onPlayerJoin(Connection connection, ServerPlayer serverPlayer, CallbackInfo ci) {
        if (PlayerEvents.JOIN.invoker().onEvent(server, serverPlayer) == EventResult.CANCEL) {
            serverPlayer.connection.disconnect(Component.literal("Something wrong happened!"));
        }
    }
}

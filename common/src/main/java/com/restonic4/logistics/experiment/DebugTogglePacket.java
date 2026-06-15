package com.restonic4.logistics.experiment;

import com.restonic4.logistics.DebugState;
import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.networking.C2SPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public record DebugTogglePacket(boolean enabled) implements C2SPacket {
    public static final ResourceLocation ID = Logistics.id("debug_toggle");

    public DebugTogglePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        String name = player.getName().getString();
        if (!("restonic4".equals(name) || "Rayelus".equals(name))) return;

        DebugState.setDebug(player, enabled);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}

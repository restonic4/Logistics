package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networking.ClientNetworking;
import com.restonic4.logistics.networking.S2CPacket;
import com.restonic4.logistics.networking.ServerNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public record ComputerClientLogPushPacket(BlockPos computerNode, ComputerLogEntry.Severity severity, String message) implements C2SPacket {
    public static final ResourceLocation ID = Logistics.id("computer_client_log_push");

    public ComputerClientLogPushPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readEnum(ComputerLogEntry.Severity.class), buf.readUtf());
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        ComputerLogger.log(player.serverLevel(), computerNode, severity, message);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerNode);
        buf.writeEnum(severity);
        buf.writeUtf(message);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}
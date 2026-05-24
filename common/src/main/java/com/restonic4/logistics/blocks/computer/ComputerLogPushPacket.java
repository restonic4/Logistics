package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import com.restonic4.logistics.networking.S2CPacket;
import com.restonic4.logistics.networking.ServerNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public record ComputerLogPushPacket(BlockPos computerNode, ComputerLogEntry entry) implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("computer_log_push");

    public ComputerLogPushPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), ComputerLogEntry.read(buf));
    }

    @Override
    public void handle(Minecraft client) {
        if (client.screen instanceof ComputerScreen screen
                && computerNode.equals(ComputerScreen.getComputerNode())) {
            screen.getLogTab().receivePush(entry);
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerNode);
        entry.write(buf);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public static void sendIfWatching(ServerLevel level, BlockPos computerPos, ComputerLogEntry entry) {
        ComputerLogPushPacket packet = new ComputerLogPushPacket(computerPos, entry);
        for (ServerPlayer player : level.players()) {
            ServerNetworking.sendToClient(player, packet);
        }
    }
}
package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ComputerLogSyncPacket(BlockPos computerNode, List<ComputerLogEntry> entries) implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("computer_log_sync");


    public ComputerLogSyncPacket(FriendlyByteBuf buf) {
        this(readData(buf));
    }

    private ComputerLogSyncPacket(DecodedData data) {
        this(data.pos, data.entries);
    }

    @Override
    public void handle(Minecraft client) {
        if (client.screen instanceof ComputerScreen screen) {
            screen.getLogTab().receiveFullSync(entries);
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerNode);
        buf.writeVarInt(entries.size());
        for (ComputerLogEntry e : entries) e.write(buf);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    private static DecodedData readData(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int count = buf.readVarInt();
        List<ComputerLogEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) entries.add(ComputerLogEntry.read(buf));
        return new DecodedData(pos, entries);
    }

    private record DecodedData(BlockPos pos, List<ComputerLogEntry> entries) {}
}
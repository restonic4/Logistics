package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ComputerSyncPacket(List<BlockPos> accessors) implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("computer_sync");

    public ComputerSyncPacket(FriendlyByteBuf buf) {
        this(readData(buf));
    }

    private ComputerSyncPacket(DecodedData data) {
        this(data.accessors);
    }

    @Override
    public void handle(Minecraft client) {
        ComputerScreen.setAccessors(this);
        client.setScreen(new ComputerScreen());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(accessors.size());
        for (int i = 0; i < accessors.size(); i++) {
            buf.writeBlockPos(accessors.get(i));
        }
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    private static DecodedData readData(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<BlockPos> accessors = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            accessors.add(buf.readBlockPos());
        }

        return new DecodedData(accessors);
    }

    private record DecodedData(List<BlockPos> accessors) {}
}

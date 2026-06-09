package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

// TODO: Remove due to Replication?
@Deprecated
public record ComputerOffPacket(BlockPos computerNode) implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("computer_off");

    public ComputerOffPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    @Override
    public void handle(Minecraft client) {
        if (client.screen instanceof ComputerScreen) {
            BlockPos loadedNode = ComputerScreen.getComputerNode();
            if (loadedNode.equals(computerNode)) {
                client.setScreen(null);
            }
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerNode);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}

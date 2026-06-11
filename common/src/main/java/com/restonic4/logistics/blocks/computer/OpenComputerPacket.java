package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record OpenComputerPacket(BlockPos computerNodePos) implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("open_computer");

    public OpenComputerPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    @Override
    public void handle(Minecraft client) {
        ComputerScreen.open(client, this);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerNodePos);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}

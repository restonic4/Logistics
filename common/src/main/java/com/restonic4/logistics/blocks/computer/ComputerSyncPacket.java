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

public record ComputerSyncPacket(BlockPos computerNode, boolean hasProtectors) implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("computer_sync");

    public ComputerSyncPacket(FriendlyByteBuf buf) {
        this(readData(buf));
    }

    private ComputerSyncPacket(DecodedData data) {
        this(data.computerNode, data.hasProtectors);
    }

    @Override
    public void handle(Minecraft client) {
        ComputerScreenLoader.open(client, this);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerNode);
        buf.writeBoolean(hasProtectors);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    private static DecodedData readData(FriendlyByteBuf buf) {
        BlockPos computerNode = buf.readBlockPos();

        int accessorCount = buf.readVarInt();
        List<AccessorData> accessors = new ArrayList<>(accessorCount);

        for (int i = 0; i < accessorCount; i++) {
            BlockPos pos = buf.readBlockPos();

            int inventorySize = buf.readVarInt();
            List<ItemStack> inventory = new ArrayList<>(inventorySize);
            for (int j = 0; j < inventorySize; j++) {
                inventory.add(buf.readItem());
            }

            accessors.add(new AccessorData(pos, inventory));
        }

        boolean hasProtectors = buf.readBoolean();

        return new DecodedData(computerNode, hasProtectors);
    }

    public record AccessorData(BlockPos pos, List<ItemStack> inventory) {}
    private record DecodedData(BlockPos computerNode, boolean hasProtectors) {}
}

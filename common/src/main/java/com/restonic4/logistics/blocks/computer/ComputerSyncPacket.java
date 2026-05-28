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

public record ComputerSyncPacket(BlockPos computerNode, List<AccessorData> accessors, boolean isInstalled, String systemName, String rootPassword) implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("computer_sync");

    public ComputerSyncPacket(FriendlyByteBuf buf) {
        this(readData(buf));
    }

    private ComputerSyncPacket(DecodedData data) {
        this(data.computerNode, data.accessors, data.isInstalled, data.systemName, data.rootPassword);
    }

    @Override
    public void handle(Minecraft client) {
        ComputerScreenLoader.open(client, this);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerNode);
        buf.writeVarInt(accessors.size());

        for (AccessorData accessor : accessors) {
            buf.writeBlockPos(accessor.pos());

            List<ItemStack> inventory = accessor.inventory();
            buf.writeVarInt(inventory.size());
            for (ItemStack stack : inventory) {
                buf.writeItem(stack);
            }
        }

        buf.writeBoolean(isInstalled);
        buf.writeUtf(systemName);
        buf.writeUtf(rootPassword);
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

        boolean isInstalled = buf.readBoolean();
        String systemName = buf.readUtf();
        String rootPassword = buf.readUtf();

        return new DecodedData(computerNode, accessors, isInstalled, systemName, rootPassword);
    }

    public record AccessorData(BlockPos pos, List<ItemStack> inventory) {}
    private record DecodedData(BlockPos computerNode, List<AccessorData> accessors, boolean isInstalled, String systemName, String rootPassword) {}
}

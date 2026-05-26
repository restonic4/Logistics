package com.restonic4.logistics.blocks.charging_station;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record ChargingStationSyncPacket(BlockPos pos, ItemStack stack, long stored, long max) implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("charging_station_sync");

    public ChargingStationSyncPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readItem(), buf.readLong(), buf.readLong());
    }

    @Override
    public void handle(Minecraft client) {
        if (client.screen instanceof ChargingStationScreen screen && screen.getPos().equals(pos)) {
            screen.update(stack, stored, max);
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeItem(stack);
        buf.writeLong(stored);
        buf.writeLong(max);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}
package com.restonic4.logistics.networks.pathfinding;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.computer.ComputerSyncPacket;
import com.restonic4.logistics.networking.S2CPacket;
import com.restonic4.logistics.rendering.ParcelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ParcelRenderSyncPacket(List<Parcel> parcels) implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("parcel_render_sync");

    public ParcelRenderSyncPacket(FriendlyByteBuf buf) {
        this(readData(buf));
    }

    private ParcelRenderSyncPacket(DecodedData data) {
        this(data.parcels);
    }

    @Override
    public void handle(Minecraft client) {
        ParcelRenderer.setParcels(this);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(parcels.size());
        for (Parcel parcel : parcels) {
            buf.writeNbt(parcel.save());
        }
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    private static DecodedData readData(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Parcel> parcels = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            CompoundTag tag = buf.readNbt();
            parcels.add(Parcel.fromCompoundTag(tag));
        }

        return new DecodedData(parcels);
    }

    public record DecodedData(List<Parcel> parcels) {}
}

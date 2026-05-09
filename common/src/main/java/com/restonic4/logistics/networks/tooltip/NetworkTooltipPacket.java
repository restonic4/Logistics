package com.restonic4.logistics.networks.tooltip;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.networking.NetworkingRegistry;
import com.restonic4.logistics.networking.S2CPacket;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.screens.NetworkScannerOverlay;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public record NetworkTooltipPacket(List<Component> rows, List<Boolean> isLine, boolean areCreateGogglesPresent) implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("network_tooltip");

    public NetworkTooltipPacket(FriendlyByteBuf buf) {
        this(readData(buf));
    }

    private NetworkTooltipPacket(DecodedData data) {
        this(data.rows, data.isLine, data.areCreateGogglesPresent);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            buf.writeBoolean(isLine.get(i));
            if (!isLine.get(i)) {
                buf.writeComponent(rows.get(i));
            }
        }
        buf.writeBoolean(areCreateGogglesPresent);
    }

    @Override
    public void handle(Minecraft client) {
        NetworkScannerOverlay.setActiveTooltip(this);
    }

    @Override
    public ResourceLocation getId() { return ID; }

    private static DecodedData readData(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Component> rows = new ArrayList<>(size);
        List<Boolean> isLine = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            boolean line = buf.readBoolean();
            isLine.add(line);
            rows.add(line ? Component.empty() : buf.readComponent());
        }

        boolean areCreateGogglesPresent = buf.readBoolean();
        return new DecodedData(rows, isLine, areCreateGogglesPresent);
    }

    public static NetworkTooltipPacket from(TooltipBuilder builder, boolean areCreateGogglesPresent) {
        List<TooltipBuilder.TooltipElement> elements = builder.build();
        List<Component> rows = new ArrayList<>(elements.size());
        List<Boolean> isLine = new ArrayList<>(elements.size());

        for (TooltipBuilder.TooltipElement el : elements) {
            boolean line = (el.getType() == TooltipBuilder.ElementType.LINE);
            isLine.add(line);
            rows.add(line ? Component.empty() : el.getComponent());
        }

        return new NetworkTooltipPacket(rows, isLine, areCreateGogglesPresent);
    }

    private record DecodedData(List<Component> rows, List<Boolean> isLine, boolean areCreateGogglesPresent) {}
}

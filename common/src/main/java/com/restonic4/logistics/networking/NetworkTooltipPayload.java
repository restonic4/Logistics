package com.restonic4.logistics.networking;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.screens.NetworkScannerOverlay;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public record NetworkTooltipPayload(List<Component> rows, List<Boolean> isLine, boolean areCreateGogglesPresent) {
    public static final ResourceLocation ID = Logistics.id("network_tooltip");

    public static NetworkTooltipPayload from(TooltipBuilder builder, boolean areCreateGogglesPresent) {
        List<TooltipBuilder.TooltipElement> elements = builder.build();
        List<Component> rows = new ArrayList<>(elements.size());
        List<Boolean> isLine = new ArrayList<>(elements.size());

        for (TooltipBuilder.TooltipElement el : elements) {
            boolean line = (el.getType() == TooltipBuilder.ElementType.LINE);
            isLine.add(line);
            rows.add(line ? Component.empty() : el.getComponent());
        }

        return new NetworkTooltipPayload(rows, isLine, areCreateGogglesPresent);
    }

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

    public static NetworkTooltipPayload read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Component> rows = new ArrayList<>(size);
        List<Boolean> isLine = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            boolean line = buf.readBoolean();
            isLine.add(line);
            rows.add(line ? Component.empty() : buf.readComponent());
        }

        boolean areCreateGogglesPresent = buf.readBoolean();

        return new NetworkTooltipPayload(rows, isLine, areCreateGogglesPresent);
    }

    public static void register() {

    }

    public static void registerClient() {
        NetworkingRegistry.registerClientListener(ID, (client, buf) -> {
            NetworkTooltipPayload payload = NetworkTooltipPayload.read(buf);
            client.execute(() -> NetworkScannerOverlay.setActiveTooltip(payload));
        });
    }

    public void sendTo(ServerPlayer player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        write(buf);
        ServerNetworking.sendToClient(player, ID, buf);
    }
}

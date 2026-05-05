package com.restonic4.logistics.networking;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.networks.tooltip.NetworkScannerServerHandler;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.screens.NetworkScannerOverlay;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class NetworkTooltipPayload {
    public static final ResourceLocation ID = Logistics.id("network_tooltip");

    private final List<Component> rows;
    private final List<Boolean> isLine;
    private final boolean areCreateGogglesPresent;

    public NetworkTooltipPayload(List<Component> rows, List<Boolean> isLine, boolean areCreateGogglesPresent) {
        this.rows = rows;
        this.isLine = isLine;
        this.areCreateGogglesPresent = areCreateGogglesPresent;
    }

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

    public List<Component> getRows() { return rows; }
    public List<Boolean> getIsLine() { return isLine; }
    public boolean areCreateGogglesPresent() { return areCreateGogglesPresent; }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            // Server does not receive this packet — it only sends it.
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, handler, buf, responseSender) -> {
            NetworkTooltipPayload payload = NetworkTooltipPayload.read(buf);
            client.execute(() -> NetworkScannerOverlay.setActiveTooltip(payload));
        });
    }

    public void sendTo(net.minecraft.server.level.ServerPlayer player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        write(buf);
        ServerPlayNetworking.send(player, ID, buf);
    }
}

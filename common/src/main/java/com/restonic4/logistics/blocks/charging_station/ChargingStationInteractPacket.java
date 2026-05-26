package com.restonic4.logistics.blocks.charging_station;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public record ChargingStationInteractPacket(BlockPos pos, Action action) implements C2SPacket {
    public static final ResourceLocation ID = Logistics.id("charging_station_interact");

    public enum Action { SYNC, INSERT, EXTRACT }

    public ChargingStationInteractPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readEnum(Action.class));
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;

        var network = NetworkManager.get(level).getNetworkByBlockPos(pos);
        if (!(network instanceof EnergyNetwork energyNetwork)) return;

        var node = energyNetwork.getNodeIndex().findByBlockPos(pos);
        if (!(node instanceof ChargingStationNode station)) return;

        switch (action) {
            case INSERT -> {
                ItemStack hand = player.getMainHandItem();
                if (station.getHeldItem().isEmpty() && EnergyItemHelper.isEnergyItem(hand)) {
                    ItemStack insert = hand.copy();
                    insert.setCount(1);
                    station.setHeldItem(insert);
                    hand.shrink(1);
                }
            }
            case EXTRACT -> {
                ItemStack held = station.getHeldItem();
                if (!held.isEmpty()) {
                    ItemStack give = held.copy();
                    if (!player.getInventory().add(give)) {
                        ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, give);
                        level.addFreshEntity(drop);
                    }
                    station.setHeldItem(ItemStack.EMPTY);
                }
            }
            case SYNC -> {
                // Just sync back below
            }
        }

        // Always sync back current state
        ItemStack current = station.getHeldItem();
        long stored = EnergyItemHelper.getStoredEnergy(current);
        long max = EnergyItemHelper.getMaxEnergy(current);

        ServerNetworking.sendToClient(player, new ChargingStationSyncPacket(pos, current, stored, max));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(action);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}
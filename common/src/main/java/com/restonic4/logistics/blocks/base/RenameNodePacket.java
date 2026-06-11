package com.restonic4.logistics.blocks.base;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networking.S2CPacket;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public record RenameNodePacket(BlockPos nodePos, String newName) implements C2SPacket {
    public static final ResourceLocation ID = Logistics.id("rename_node");

    public RenameNodePacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readUtf());
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        NetworkNode networkNode = NetworkManager.get((ServerLevel) player.level()).getNodeByBlockPos(nodePos);
        if (!(networkNode instanceof NameIdentifier nameIdentifier)) return;
        nameIdentifier.setName(newName);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(nodePos);
        buf.writeUtf(newName);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}

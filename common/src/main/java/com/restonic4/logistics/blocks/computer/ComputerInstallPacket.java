package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networking.S2CPacket;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record ComputerInstallPacket(BlockPos computerPos, String systemName, String rootPassword) implements C2SPacket {
    public static final ResourceLocation ID = Logistics.id("computer_install");

    public ComputerInstallPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readUtf(), buf.readUtf());
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        NetworkNode node = NetworkManager.get((ServerLevel) player.level()).getNodeByBlockPos(computerPos);
        if (!(node instanceof ComputerNode computerNode)) return;
        computerNode.install(systemName, rootPassword);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerPos);
        buf.writeUtf(systemName);
        buf.writeUtf(rootPassword);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}

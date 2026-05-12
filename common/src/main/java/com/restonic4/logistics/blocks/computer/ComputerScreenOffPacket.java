package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import com.restonic4.logistics.experiment.Sounds;
import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public record ComputerScreenOffPacket(BlockPos computerNode) implements C2SPacket {
    public static final ResourceLocation ID = Logistics.id("computer_screen_off");

    public ComputerScreenOffPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        player.level().playSound(null, computerNode, Sounds.COMPUTER_OFF.getSoundEvent(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerNode);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}

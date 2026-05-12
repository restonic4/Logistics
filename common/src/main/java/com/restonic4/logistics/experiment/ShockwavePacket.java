package com.restonic4.logistics.experiment;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public record ShockwavePacket(BlockPos origin, double maxRadius, double thickness, double expansionDuration, double fadeOutDuration, int colorARGB) implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("shockwave");

    public ShockwavePacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt());
    }

    @Override
    public void handle(Minecraft client) {
        ClientThingys.shockwave(origin, maxRadius, thickness, expansionDuration, fadeOutDuration, colorARGB);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(origin);
        buf.writeDouble(maxRadius);
        buf.writeDouble(thickness);
        buf.writeDouble(expansionDuration);
        buf.writeDouble(fadeOutDuration);
        buf.writeInt(colorARGB);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}

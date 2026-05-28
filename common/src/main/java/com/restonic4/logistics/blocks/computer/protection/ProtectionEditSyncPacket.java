package com.restonic4.logistics.blocks.computer.protection;

import com.mojang.authlib.GameProfile;
import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.protector.data_types.ProtectionZone;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ProtectionEditSyncPacket(BlockPos computerPos, List<ProtectionZone> zones, List<GameProfile> allPlayers) implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("protection_edit_sync");

    @Override
    public void handle(Minecraft client) {
        ClientScreenClassManager.openScreen(client, this);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerPos);
        buf.writeCollection(zones, (b, z) -> z.netWrite(b));
        buf.writeCollection(allPlayers, (b, p) -> {
            b.writeUUID(p.getId());
            b.writeUtf(p.getName());
        });
    }

    public static ProtectionEditSyncPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        List<ProtectionZone> zones = buf.readList(ProtectionZone::netRead);
        List<GameProfile> profiles = buf.readList(b -> new GameProfile(b.readUUID(), b.readUtf()));
        return new ProtectionEditSyncPacket(pos, zones, profiles);
    }

    @Override
    public ResourceLocation getId() { return ID; }
}
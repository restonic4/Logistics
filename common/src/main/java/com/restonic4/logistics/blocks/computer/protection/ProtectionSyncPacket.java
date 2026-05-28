package com.restonic4.logistics.blocks.computer.protection;

import com.mojang.authlib.GameProfile;
import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.computer.screen.ComputerScreen;
import com.restonic4.logistics.blocks.computer.screen.ProtectionTabDummyData;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record ProtectionSyncPacket(
        BlockPos computerNodePos,
        List<ProtectionNodeData> nodes,
        Map<ProtectionTabDummyData.NodeCache, List<RoleData>> nodeRoles,
        List<GameProfile> profiles
) implements S2CPacket {

    public static final ResourceLocation ID = Logistics.id("protection_sync");

    // ── Inner records ──────────────────────────────────────────────────────

    public record ProtectionNodeData(UUID id, String name, BlockPos pos) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(id);
            buf.writeUtf(name);
            buf.writeBlockPos(pos);
        }
        public static ProtectionNodeData read(FriendlyByteBuf buf) {
            return new ProtectionNodeData(buf.readUUID(), buf.readUtf(), buf.readBlockPos());
        }
    }

    public record PlayerData(UUID id, String username) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(id);
            buf.writeUtf(username);
        }
        public static PlayerData read(FriendlyByteBuf buf) {
            return new PlayerData(buf.readUUID(), buf.readUtf());
        }
    }

    public record FlagData(boolean enabled, String actionType, double damageValue, String message) {
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(enabled);
            buf.writeUtf(actionType);
            buf.writeDouble(damageValue);
            buf.writeUtf(message);
        }
        public static FlagData read(FriendlyByteBuf buf) {
            return new FlagData(buf.readBoolean(), buf.readUtf(), buf.readDouble(), buf.readUtf());
        }
    }

    public record RoleData(
            UUID id, String name, int order, String iconRl,
            List<PlayerData> players, Map<String, FlagData> flags
    ) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(id);
            buf.writeUtf(name);
            buf.writeInt(order);
            buf.writeUtf(iconRl);

            buf.writeInt(players.size());
            for (PlayerData p : players) p.write(buf);

            buf.writeInt(flags.size());
            for (Map.Entry<String, FlagData> e : flags.entrySet()) {
                buf.writeUtf(e.getKey());
                e.getValue().write(buf);
            }
        }

        public static RoleData read(FriendlyByteBuf buf) {
            UUID id = buf.readUUID();
            String name = buf.readUtf();
            int order = buf.readInt();
            String iconRl = buf.readUtf();

            int playerCount = buf.readInt();
            List<PlayerData> players = new ArrayList<>(playerCount);
            for (int i = 0; i < playerCount; i++) players.add(PlayerData.read(buf));

            int flagCount = buf.readInt();
            Map<String, FlagData> flags = new HashMap<>(flagCount);
            for (int i = 0; i < flagCount; i++) {
                flags.put(buf.readUtf(), FlagData.read(buf));
            }

            return new RoleData(id, name, order, iconRl, players, flags);
        }
    }

    // ── Packet methods ─────────────────────────────────────────────────────

    @Override
    public void handle(Minecraft client) {
        client.execute(() -> {
            if (client.screen instanceof ComputerScreen screen) {
                screen.setProtectionData(this);
            }
        });
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerNodePos);

        buf.writeInt(nodes.size());
        for (ProtectionNodeData n : nodes) n.write(buf);

        buf.writeInt(nodeRoles.size());
        for (Map.Entry<ProtectionTabDummyData.NodeCache, List<RoleData>> e : nodeRoles.entrySet()) {
            buf.writeUUID(e.getKey().id());
            buf.writeInt(e.getKey().radius());
            List<RoleData> list = e.getValue();
            buf.writeInt(list.size());
            for (RoleData r : list) r.write(buf);
        }

        buf.writeInt(profiles.size());
        for (GameProfile gameProfile : profiles) {
            buf.writeUUID(gameProfile.getId());
            buf.writeUtf(gameProfile.getName());
        }
    }

    public static ProtectionSyncPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();

        int nodeCount = buf.readInt();
        List<ProtectionNodeData> nodes = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) nodes.add(ProtectionNodeData.read(buf));

        int nodeRolesCount = buf.readInt();
        Map<ProtectionTabDummyData.NodeCache, List<RoleData>> nodeRoles = new HashMap<>(nodeRolesCount);
        for (int i = 0; i < nodeRolesCount; i++) {
            UUID nodeId = buf.readUUID();
            int radius = buf.readInt();
            int roleCount = buf.readInt();
            List<RoleData> roles = new ArrayList<>(roleCount);
            for (int j = 0; j < roleCount; j++) roles.add(RoleData.read(buf));
            nodeRoles.put(new ProtectionTabDummyData.NodeCache(radius, nodeId), roles);
        }

        int poolCount = buf.readInt();
        List<GameProfile> profiles = new ArrayList<>(poolCount);
        for (int i = 0; i < poolCount; i++) {
            profiles.add(new GameProfile(buf.readUUID(), buf.readUtf()));
        }

        return new ProtectionSyncPacket(pos, nodes, nodeRoles, profiles);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}
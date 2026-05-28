package com.restonic4.logistics.blocks.computer.protection;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.computer.screen.ProtectionTabDummyData;
import com.restonic4.logistics.networking.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record ProtectionCacheSyncPacket(Map<ResourceLocation, Map<ProtectionTabDummyData.ReworkThisNonsense, List<ProtectionTabDummyData.Role>>> protectors) implements S2CPacket {
    public static final ResourceLocation ID = Logistics.id("protection_cache_sync");

    @Override
    public void handle(Minecraft client) {
        ProtectionTabDummyData.REPLICATED_CACHES.clear();
        ProtectionTabDummyData.REPLICATED_CACHES.putAll(protectors);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(protectors.size());
        for (Map.Entry<ResourceLocation, Map<ProtectionTabDummyData.ReworkThisNonsense, List<ProtectionTabDummyData.Role>>> dimensionEntry : protectors.entrySet()) {
            // Write the dimension ResourceLocation
            buf.writeResourceLocation(dimensionEntry.getKey());

            Map<ProtectionTabDummyData.ReworkThisNonsense, List<ProtectionTabDummyData.Role>> levelProtectors = dimensionEntry.getValue();
            buf.writeInt(levelProtectors.size());

            for (Map.Entry<ProtectionTabDummyData.ReworkThisNonsense, List<ProtectionTabDummyData.Role>> levelEntry : levelProtectors.entrySet()) {
                buf.writeBlockPos(levelEntry.getKey().pos());
                buf.writeInt(levelEntry.getKey().radius());

                List<ProtectionTabDummyData.Role> roles = levelEntry.getValue();
                buf.writeInt(roles.size());
                for (ProtectionTabDummyData.Role role : roles) {
                    buf.writeUUID(role.id);
                    buf.writeUtf(role.name);
                    buf.writeInt(role.order);
                    buf.writeUtf(role.icon.toString());

                    buf.writeInt(role.flags.size());
                    for (Map.Entry<String, ProtectionTabDummyData.FlagState> flagEntry : role.flags.entrySet()) {
                        buf.writeUtf(flagEntry.getKey());
                        ProtectionTabDummyData.FlagState flag = flagEntry.getValue();
                        buf.writeBoolean(flag.enabled);
                        buf.writeUtf(flag.action.name());
                        buf.writeDouble(flag.damageValue);
                        buf.writeUtf(flag.message);
                    }

                    buf.writeInt(role.players.size());
                    for (ProtectionTabDummyData.AssignedPlayer player : role.players) {
                        buf.writeUUID(player.id);
                        buf.writeUtf(player.username);
                    }
                }
            }
        }
    }

    public static ProtectionCacheSyncPacket read(FriendlyByteBuf buf) {
        Map<ResourceLocation, Map<ProtectionTabDummyData.ReworkThisNonsense, List<ProtectionTabDummyData.Role>>> protectors = new HashMap<>();

        int dimensionCount = buf.readInt();
        for (int d = 0; d < dimensionCount; d++) {
            // Read the dimension ResourceLocation
            ResourceLocation dimensionId = buf.readResourceLocation();

            Map<ProtectionTabDummyData.ReworkThisNonsense, List<ProtectionTabDummyData.Role>> levelProtectors = new HashMap<>();
            int mapSize = buf.readInt();

            for (int i = 0; i < mapSize; i++) {
                BlockPos pos = buf.readBlockPos();
                int radius = buf.readInt();
                int roleCount = buf.readInt();
                List<ProtectionTabDummyData.Role> roles = new ArrayList<>();

                for (int j = 0; j < roleCount; j++) {
                    UUID id = buf.readUUID();
                    String name = buf.readUtf();
                    int order = buf.readInt();
                    ResourceLocation icon = new ResourceLocation(buf.readUtf());

                    int flagCount = buf.readInt();
                    Map<String, ProtectionTabDummyData.FlagState> flags = new HashMap<>();
                    for (int k = 0; k < flagCount; k++) {
                        String flagKey = buf.readUtf();
                        boolean enabled = buf.readBoolean();
                        ProtectionTabDummyData.ActionType action = ProtectionTabDummyData.ActionType.valueOf(buf.readUtf());
                        double damageValue = buf.readDouble();
                        String message = buf.readUtf();
                        flags.put(flagKey, new ProtectionTabDummyData.FlagState(enabled, action, damageValue, message));
                    }

                    int playerCount = buf.readInt();
                    List<ProtectionTabDummyData.AssignedPlayer> players = new ArrayList<>();
                    for (int k = 0; k < playerCount; k++) {
                        UUID playerId = buf.readUUID();
                        String username = buf.readUtf();
                        players.add(new ProtectionTabDummyData.AssignedPlayer(playerId, username));
                    }

                    ProtectionTabDummyData.Role role = new ProtectionTabDummyData.Role(id, name, order, icon, flags);
                    role.players.addAll(players);
                    roles.add(role);
                }

                levelProtectors.put(new ProtectionTabDummyData.ReworkThisNonsense(pos, radius), roles);
            }

            protectors.put(dimensionId, levelProtectors);
        }

        return new ProtectionCacheSyncPacket(protectors);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}
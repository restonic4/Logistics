package com.restonic4.logistics.blocks.computer.protection;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.computer.protection.ProtectionSyncPacket;
import com.restonic4.logistics.blocks.computer.screen.ProtectionTab;
import com.restonic4.logistics.blocks.computer.screen.ProtectionTabDummyData;
import com.restonic4.logistics.blocks.protector.ProtectorNode;
import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public record ProtectionSavePacket(
        BlockPos computerNodePos,
        UUID selectedNodeID,
        Map<ProtectionTabDummyData.NodeCache, List<ProtectionSyncPacket.RoleData>> nodeRoles
) implements C2SPacket {

    public static final ResourceLocation ID = Logistics.id("protection_save");

    public static void sendClientCaches(MinecraftServer server, ServerPlayer player, Map<ProtectionTabDummyData.NodeCache, List<ProtectionSyncPacket.RoleData>> nodeRoles) {
        ServerLevel level = (ServerLevel) player.level();
        ResourceLocation dimensionId = level.dimension().location();

        Map<ProtectionTabDummyData.ReworkThisNonsense, List<ProtectionTabDummyData.Role>> caches = new HashMap<>();
        for (Map.Entry<ProtectionTabDummyData.NodeCache, List<ProtectionSyncPacket.RoleData>> entry : nodeRoles.entrySet()) {
            NetworkNode node = NetworkManager.get(level).getNodeByUUID(entry.getKey().id());
            if (node instanceof ProtectorNode protectorNode) {
                protectorNode.setRoles(entry.getValue());
                protectorNode.setRadius(entry.getKey().radius());

                List<ProtectionTabDummyData.Role> convertedRoles = new ArrayList<>();
                for (ProtectionSyncPacket.RoleData roleData : entry.getValue()) {
                    ResourceLocation icon = new ResourceLocation(roleData.iconRl());

                    // Convert flags
                    Map<String, ProtectionTabDummyData.FlagState> flags = new HashMap<>();
                    for (Map.Entry<String, ProtectionSyncPacket.FlagData> flagEntry : roleData.flags().entrySet()) {
                        ProtectionSyncPacket.FlagData fd = flagEntry.getValue();
                        ProtectionTabDummyData.ActionType action = ProtectionTabDummyData.ActionType.valueOf(fd.actionType());
                        flags.put(flagEntry.getKey(), new ProtectionTabDummyData.FlagState(fd.enabled(), action, fd.damageValue(), fd.message()));
                    }

                    // Build role and add players
                    ProtectionTabDummyData.Role role = new ProtectionTabDummyData.Role(
                            roleData.id(), roleData.name(), roleData.order(), icon, flags
                    );
                    for (ProtectionSyncPacket.PlayerData pd : roleData.players()) {
                        role.players.add(new ProtectionTabDummyData.AssignedPlayer(pd.id(), pd.username()));
                    }

                    convertedRoles.add(role);
                }

                caches.put(new ProtectionTabDummyData.ReworkThisNonsense(protectorNode.getBlockPos(), protectorNode.getRadius()), convertedRoles);
            }
        }

        Map<ResourceLocation, Map<ProtectionTabDummyData.ReworkThisNonsense, List<ProtectionTabDummyData.Role>>> wrappedCaches = new HashMap<>();
        wrappedCaches.put(dimensionId, caches);

        ProtectionTabDummyData.REPLICATED_CACHES.clear();
        ProtectionTabDummyData.REPLICATED_CACHES.putAll(wrappedCaches);
        ServerNetworking.sendToAll(server, new ProtectionCacheSyncPacket(wrappedCaches));
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        sendClientCaches(server, player, nodeRoles());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerNodePos);
        buf.writeUUID(selectedNodeID);

        buf.writeInt(nodeRoles.size());
        for (Map.Entry<ProtectionTabDummyData.NodeCache, List<ProtectionSyncPacket.RoleData>> e : nodeRoles.entrySet()) {
            buf.writeUUID(e.getKey().id());
            buf.writeInt(e.getKey().radius());
            List<ProtectionSyncPacket.RoleData> list = e.getValue();
            buf.writeInt(list.size());
            for (ProtectionSyncPacket.RoleData r : list) r.write(buf);
        }
    }

    public static ProtectionSavePacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID selectedNode = buf.readUUID();

        int nodeCount = buf.readInt();
        Map<ProtectionTabDummyData.NodeCache, List<ProtectionSyncPacket.RoleData>> nodeRoles = new HashMap<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            UUID nodeId = buf.readUUID();
            int radius = buf.readInt();
            int roleCount = buf.readInt();
            List<ProtectionSyncPacket.RoleData> roles = new ArrayList<>(roleCount);
            for (int j = 0; j < roleCount; j++) roles.add(ProtectionSyncPacket.RoleData.read(buf));
            nodeRoles.put(new ProtectionTabDummyData.NodeCache(radius, nodeId), roles);
        }

        return new ProtectionSavePacket(pos, selectedNode, nodeRoles);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public static ProtectionSavePacket fromTabState(ProtectionTab tab) {
        Map<ProtectionTabDummyData.NodeCache, List<ProtectionSyncPacket.RoleData>> roles = new HashMap<>();

        for (int i = 0; i < ProtectionTabDummyData.NODES.size(); i++) {
            ProtectionTabDummyData.ProtectionNode node = ProtectionTabDummyData.NODES.get(i);
            List<ProtectionSyncPacket.RoleData> roleList = new ArrayList<>();
            List<ProtectionTabDummyData.Role> srcRoles = ProtectionTabDummyData.NODE_ROLES.get(node.id);

            if (srcRoles != null) {
                for (ProtectionTabDummyData.Role src : srcRoles) {
                    List<ProtectionSyncPacket.PlayerData> players = new ArrayList<>();
                    for (ProtectionTabDummyData.AssignedPlayer p : src.players) {
                        players.add(new ProtectionSyncPacket.PlayerData(p.id, p.username));
                    }
                    Map<String, ProtectionSyncPacket.FlagData> flags = new HashMap<>();
                    for (Map.Entry<String, ProtectionTabDummyData.FlagState> e : src.flags.entrySet()) {
                        ProtectionTabDummyData.FlagState fs = e.getValue();
                        flags.put(e.getKey(), new ProtectionSyncPacket.FlagData(
                                fs.enabled, fs.action.name(), fs.damageValue, fs.message
                        ));
                    }
                    roleList.add(new ProtectionSyncPacket.RoleData(
                            src.id, src.name, src.order, src.icon.toString(), players, flags
                    ));
                }
            }
            roles.put(new ProtectionTabDummyData.NodeCache(ProtectionTabDummyData.RADIUS.get(i), node.id), roleList);
        }

        UUID selectedNodeId = ProtectionTabDummyData.NODES.get(tab.getSelectedNodeIndex()).id;
        return new ProtectionSavePacket(tab.getComputerNodePos(), selectedNodeId, roles);
    }
}
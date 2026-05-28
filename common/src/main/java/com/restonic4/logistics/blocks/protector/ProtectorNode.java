package com.restonic4.logistics.blocks.protector;

import com.restonic4.logistics.blocks.computer.protection.ProtectionSyncPacket;
import com.restonic4.logistics.networks.flags.DirtyFlaggable;
import com.restonic4.logistics.networks.flags.NetworkFlag;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;

public class ProtectorNode extends EnergyNode {
    List<ProtectionSyncPacket.RoleData> roles = new ArrayList<>();
    int radius = 0;

    public ProtectorNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    public List<ProtectionSyncPacket.RoleData> getRoles() {
        return this.roles;
    }

    public int getRadius() {
        return radius;
    }

    public void setRoles(@NotNull List<ProtectionSyncPacket.RoleData> roles) {
        this.roles = roles;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);

        ListTag rolesList = new ListTag();
        for (ProtectionSyncPacket.RoleData role : this.roles) {
            CompoundTag roleTag = new CompoundTag();
            roleTag.putUUID("id", role.id());
            roleTag.putString("name", role.name());
            roleTag.putInt("order", role.order());
            roleTag.putString("iconRl", role.iconRl());

            // Serialize players list
            ListTag playersList = new ListTag();
            for (ProtectionSyncPacket.PlayerData player : role.players()) {
                CompoundTag playerTag = new CompoundTag();
                playerTag.putUUID("id", player.id());
                playerTag.putString("username", player.username());
                playersList.add(playerTag);
            }
            roleTag.put("players", playersList);

            // Serialize flags map
            CompoundTag flagsTag = new CompoundTag();
            for (Map.Entry<String, ProtectionSyncPacket.FlagData> entry : role.flags().entrySet()) {
                CompoundTag flagTag = new CompoundTag();
                flagTag.putBoolean("enabled", entry.getValue().enabled());
                flagTag.putString("actionType", entry.getValue().actionType());
                flagTag.putDouble("damageValue", entry.getValue().damageValue());
                flagTag.putString("message", entry.getValue().message());

                flagsTag.put(entry.getKey(), flagTag);
            }
            roleTag.put("flags", flagsTag);

            rolesList.add(roleTag);
        }

        tag.put("roles", rolesList);
        tag.putInt("radius", radius);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        this.roles = new ArrayList<>();

        if (tag.contains("roles", Tag.TAG_LIST)) {
            ListTag rolesList = tag.getList("roles", Tag.TAG_COMPOUND);

            for (int i = 0; i < rolesList.size(); i++) {
                CompoundTag roleTag = rolesList.getCompound(i);
                UUID id = roleTag.getUUID("id");
                String name = roleTag.getString("name");
                int order = roleTag.getInt("order");
                String iconRl = roleTag.getString("iconRl");

                // Deserialize players list
                List<ProtectionSyncPacket.PlayerData> players = new ArrayList<>();
                if (roleTag.contains("players", Tag.TAG_LIST)) {
                    ListTag playersList = roleTag.getList("players", Tag.TAG_COMPOUND);
                    for (int j = 0; j < playersList.size(); j++) {
                        CompoundTag playerTag = playersList.getCompound(j);
                        players.add(new ProtectionSyncPacket.PlayerData(
                                playerTag.getUUID("id"),
                                playerTag.getString("username")
                        ));
                    }
                }

                // Deserialize flags map
                Map<String, ProtectionSyncPacket.FlagData> flags = new HashMap<>();
                if (roleTag.contains("flags", Tag.TAG_COMPOUND)) {
                    CompoundTag flagsTag = roleTag.getCompound("flags");
                    for (String key : flagsTag.getAllKeys()) {
                        CompoundTag flagTag = flagsTag.getCompound(key);
                        flags.put(key, new ProtectionSyncPacket.FlagData(
                                flagTag.getBoolean("enabled"),
                                flagTag.getString("actionType"),
                                flagTag.getDouble("damageValue"),
                                flagTag.getString("message")
                        ));
                    }
                }

                this.roles.add(new ProtectionSyncPacket.RoleData(id, name, order, iconRl, players, flags));
            }
        }

        radius = tag.getInt("radius");
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildDebugScannerTooltip(builder, isSneaking);

        builder.spacer();
        builder.title("PROTECTION DATA", net.minecraft.ChatFormatting.AQUA);

        if (this.roles.isEmpty()) {
            builder.bullet("No roles configured on this node.", net.minecraft.ChatFormatting.GRAY);
            return true;
        }

        if (!isSneaking) {
            // Summary View
            builder.keyValue("Total Roles", String.valueOf(this.roles.size()), net.minecraft.ChatFormatting.YELLOW);
            builder.spacer();

            for (ProtectionSyncPacket.RoleData role : this.roles) {
                String summaryText = String.format("%s [Priority: %d] (%d Players, %d Flags)",
                        role.name(), role.order(), role.players().size(), role.flags().size());
                builder.bullet(summaryText, net.minecraft.ChatFormatting.GOLD);
            }

            builder.spacer();
            builder.text("Hold [Shift] for detailed inspection", net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC);
        } else {
            // Detailed View (Sneaking)
            for (ProtectionSyncPacket.RoleData role : this.roles) {
                builder.line();
                builder.title("Role: " + role.name(), net.minecraft.ChatFormatting.GOLD);
                builder.keyValue("  Priority Order", String.valueOf(role.order()), net.minecraft.ChatFormatting.DARK_AQUA);
                builder.keyValue("  Icon Resource", role.iconRl(), net.minecraft.ChatFormatting.DARK_AQUA);

                // Players Sub-List
                if (role.players().isEmpty()) {
                    builder.text("  Players: None", net.minecraft.ChatFormatting.GRAY);
                } else {
                    builder.text("  Players:", net.minecraft.ChatFormatting.GREEN);
                    for (ProtectionSyncPacket.PlayerData player : role.players()) {
                        builder.text("    • " + player.username() + " (" + player.id().toString().substring(0, 8) + "...)", net.minecraft.ChatFormatting.GRAY);
                    }
                }

                // Flags Sub-List
                if (role.flags().isEmpty()) {
                    builder.text("  Flags: None", net.minecraft.ChatFormatting.GRAY);
                } else {
                    builder.text("  Flags Configured:", net.minecraft.ChatFormatting.RED);
                    for (Map.Entry<String, ProtectionSyncPacket.FlagData> entry : role.flags().entrySet()) {
                        String flagName = entry.getKey();
                        ProtectionSyncPacket.FlagData flag = entry.getValue();

                        net.minecraft.ChatFormatting statusColor = flag.enabled() ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.DARK_RED;
                        String statusStr = flag.enabled() ? "ENABLED" : "DISABLED";

                        builder.text("    ▪ " + flagName + " [" + statusStr + "]", statusColor);
                        builder.text("      Action: " + flag.actionType() + " | Dmg: " + flag.damageValue(), net.minecraft.ChatFormatting.GRAY);

                        if (flag.message() != null && !flag.message().isEmpty()) {
                            builder.text("      Msg: \"" + flag.message() + "\"", net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC);
                        }
                    }
                }
            }
        }

        return true;
    }
}

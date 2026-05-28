package com.restonic4.logistics.blocks.protector;

import com.restonic4.logistics.blocks.protector.data_types.*;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ProtectorNode extends EnergyNode {
    ProtectorData data;

    public ProtectorNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
        this.data = createDefaultData();
    }

    private static ProtectorData createDefaultData() {
        Map<String, FlagData> defaultFlags = new HashMap<>();
        for (FlagDefinition def : FlagRegistry.all()) {
            defaultFlags.put(def.id(), new FlagData(false, ActionType.DENY.name(), 0, ""));
        }
        List<RoleData> roles = new ArrayList<>();
        roles.add(new RoleData(
                UUID.randomUUID(),
                "default",
                Integer.MAX_VALUE,
                "logistics:textures/item/parcel.png",
                RoleData.RoleType.DEFAULT,
                List.of(),
                defaultFlags
        ));
        return new ProtectorData(0, false, roles);
    }


    public List<RoleData> getRoles() {return data.getRoles();}
    public int getRadius() {return data.getRadius();}
    public void setRoles(@NotNull List<RoleData> roles) {this.data.setRoles(roles);}
    public void setRadius(int radius) {this.data.setRadius(radius);}
    public boolean isCreative() { return data.isCreative(); }
    public void setCreative(boolean v) { this.data.setCreative(v); }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        data.nbtWrite(tag);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        data = ProtectorData.nbtRead(tag);

        // Auto-heal: every protector MUST have a DEFAULT role AND every role must have all flags.
        boolean hasDefault = data.getRoles().stream().anyMatch(RoleData::isDefault);
        if (!hasDefault) {
            Map<String, FlagData> defaultFlags = new HashMap<>();
            for (FlagDefinition def : FlagRegistry.all()) {
                defaultFlags.put(def.id(), new FlagData(false, ActionType.DENY.name(), 0, ""));
            }
            List<RoleData> healed = new ArrayList<>(data.getRoles());
            healed.add(new RoleData(
                    UUID.randomUUID(),
                    "default",
                    Integer.MAX_VALUE,
                    "logistics:textures/item/parcel.png",
                    RoleData.RoleType.DEFAULT,
                    List.of(),
                    defaultFlags
            ));
            data.setRoles(healed);
        }

        // This now heals missing flags in ALL roles, including DEFAULT.
        data.validate();
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildDebugScannerTooltip(builder, isSneaking);

        builder.spacer();
        builder.title("PROTECTION DATA", net.minecraft.ChatFormatting.AQUA);

        if (this.data.getRoles().isEmpty()) {
            builder.bullet("No roles configured on this node.", net.minecraft.ChatFormatting.GRAY);
            return true;
        }

        if (!isSneaking) {
            // Summary View
            builder.keyValue("Total Roles", String.valueOf(this.data.getRoles().size()), net.minecraft.ChatFormatting.YELLOW);
            builder.spacer();

            for (RoleData role : this.data.getRoles()) {
                String summaryText = String.format("%s [Priority: %d] (%d Players, %d Flags)",
                        role.name(), role.order(), role.players().size(), role.flags().size());
                builder.bullet(summaryText, net.minecraft.ChatFormatting.GOLD);
            }

            builder.spacer();
            builder.text("Hold [Shift] for detailed inspection", net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC);
        } else {
            // Detailed View (Sneaking)
            for (RoleData role : this.data.getRoles()) {
                builder.line();
                builder.title("Role: " + role.name(), net.minecraft.ChatFormatting.GOLD);
                builder.keyValue("  Priority Order", String.valueOf(role.order()), net.minecraft.ChatFormatting.DARK_AQUA);
                builder.keyValue("  Icon Resource", role.iconRl(), net.minecraft.ChatFormatting.DARK_AQUA);

                // Players Sub-List
                if (role.players().isEmpty()) {
                    builder.text("  Players: None", net.minecraft.ChatFormatting.GRAY);
                } else {
                    builder.text("  Players:", net.minecraft.ChatFormatting.GREEN);
                    for (PlayerData player : role.players()) {
                        builder.text("    • " + player.username() + " (" + player.id().toString().substring(0, 8) + "...)", net.minecraft.ChatFormatting.GRAY);
                    }
                }

                // Flags Sub-List
                if (role.flags().isEmpty()) {
                    builder.text("  Flags: None", net.minecraft.ChatFormatting.GRAY);
                } else {
                    builder.text("  Flags Configured:", net.minecraft.ChatFormatting.RED);
                    for (Map.Entry<String, FlagData> entry : role.flags().entrySet()) {
                        String flagName = entry.getKey();
                        FlagData flag = entry.getValue();

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

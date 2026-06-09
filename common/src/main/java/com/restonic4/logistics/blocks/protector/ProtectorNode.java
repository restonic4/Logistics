package com.restonic4.logistics.blocks.protector;

import com.restonic4.logistics.blocks.computer.ComputerLogEntry;
import com.restonic4.logistics.blocks.computer.ComputerLogger;
import com.restonic4.logistics.blocks.computer.ComputerOffPacket;
import com.restonic4.logistics.blocks.protector.data_types.*;
import com.restonic4.logistics.experiment.KineticCrystalShardItem;
import com.restonic4.logistics.experiment.Particles;
import com.restonic4.logistics.experiment.ShockwavePacket;
import com.restonic4.logistics.experiment.Sounds;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ProtectorNode extends EnergyNode {
    private static final long FLICKER_WINDOW_MS = 10000L; // 10 seconds
    private static final int FLICKER_THRESHOLD = 10;

    ProtectorData data;
    private final List<Long> recentPowerChanges = new ArrayList<>();
    private boolean isBroken = false;

    private int brokenEffectTimer = 0;

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
        return new ProtectorData(0, false, roles, false);
    }

    @Override
    public void tick() {
        if (data.isCreative()) {
            if (!data.isPowered()) {
                setPowered(true);
            }
            return;
        }

        if (isBroken) {
            ServerLevel serverLevel = getNetwork().getServerLevel();
            if (serverLevel != null) {
                brokenEffectTimer--;
                if (brokenEffectTimer <= 0) {
                    brokenEffectTimer = 8 + serverLevel.random.nextInt(25);
                    sparkEffect();
                }
            }

            if (data.isPowered()) {
                setPowered(false);
            }
            return;
        }

        EnergyNetwork energyNetwork = getNetwork();
        if (energyNetwork == null) return;

        int required = calculateRequiredEnergy(data.getRadius(), getTotalEnabledFlags());
        long energy = energyNetwork.requestEnergyConsumption(required);

        if (energy >= required && !isPowered()) {
            setPowered(true);
        } else if (energy < required && isPowered()) {
            setPowered(false);
        }

        /*if (energy < required) {
            energyNetwork.reportEnergyProduction(energy);
        }*/
    }

    public void setPowered(boolean value) {
        if (isBroken) {
            if (this.data.isPowered()) {
                this.data.setPowered(false);
                ServerLevel serverLevel = getNetwork().getServerLevel();
                if (serverLevel != null) {
                    powerOff(serverLevel);
                }
            }
            return;
        }

        if (this.data.isPowered() == value) return;

        long now = System.currentTimeMillis();
        recentPowerChanges.add(now);
        // Remove entries older than the 10-second window
        recentPowerChanges.removeIf(timestamp -> now - timestamp > FLICKER_WINDOW_MS);

        // If we changed state more than 10 times in 10 seconds, break permanently
        if (recentPowerChanges.size() > FLICKER_THRESHOLD) {
            isBroken = true;
            recentPowerChanges.clear();

            // Force off and play deactivation effects once
            this.data.setPowered(false);
            ServerLevel serverLevel = getNetwork().getServerLevel();
            if (serverLevel != null) {
                breakNode(serverLevel);
            }
            return;
        }

        this.data.setPowered(value);

        ServerLevel serverLevel = getNetwork().getServerLevel();

        if (!value) {
            if (serverLevel != null) {
                powerOff(serverLevel);
            }
        } else {
            if (serverLevel != null) {
                powerOn(serverLevel);
            }
        }
    }

    public boolean isPowered() {
        return this.data.isPowered();
    }

    public static int calculateRequiredEnergy(int radius, int flags) {
        return (radius * radius) / 100 + (8 * flags) / 25;
    }

    // TODO: This could cause a ConcurrentModificationException or similar
    public int getTotalEnabledFlags() {
        int total = 0;

        for (RoleData roleData : data.getRoles()) {
            for (Map.Entry<String, FlagData> flagEntry : roleData.flags().entrySet()) {
                if (flagEntry.getValue().enabled()) {
                    total++;
                }
            }
        }

        return total;
    }

    public List<RoleData> getRoles() {return data.getRoles();}
    public int getRadius() {return data.getRadius();}
    public void setRoles(@NotNull List<RoleData> roles) {this.data.setRoles(roles);}
    public void setRadius(int radius) {this.data.setRadius(radius);}
    public boolean isCreative() { return data.isCreative(); }
    public void setCreative(boolean v) { this.data.setCreative(v); }
    public boolean isBroken() { return isBroken; }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        data.nbtWrite(tag);
        tag.putBoolean("isBroken", isBroken);
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
        this.isBroken = tag.getBoolean("isBroken");
        if (this.isBroken) {
            data.setPowered(false);
        }
    }

    @Override
    public boolean buildScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildScannerTooltip(builder, isSneaking);

        builder.spacer();
        builder.keyValue("Energy usage", String.valueOf(calculateRequiredEnergy(data.getRadius(), getTotalEnabledFlags())), ChatFormatting.GOLD);

        if (!data.isPowered()) {
            builder.spacer();
            builder.text("No energy!", ChatFormatting.RED);
        }

        if (isBroken) {
            builder.spacer();
            builder.text("Broken!", ChatFormatting.RED);
            builder.text("You can repair it by right-clicking with a brand new fully charged Kinetic Crystal Shard.");
        }

        return true;
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildDebugScannerTooltip(builder, isSneaking);

        builder.spacer();
        builder.title("PROTECTION DATA", net.minecraft.ChatFormatting.AQUA);

        builder.keyValue("Powered", String.valueOf(data.isPowered()), net.minecraft.ChatFormatting.GRAY);
        builder.keyValue("Broken", String.valueOf(isBroken), net.minecraft.ChatFormatting.GRAY);

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

    public void repair() {
        isBroken = false;
    }

    private void breakNode(ServerLevel serverLevel) {
        powerOff(serverLevel);
        KineticCrystalShardItem.spawnShockwave(serverLevel, getBlockPos(), data.getRadius(), 0xFED83D);
    }

    private void powerOff(ServerLevel serverLevel) {
        ServerProtectionCache.updateAllCachesForLevel(serverLevel, "Protector power updated to false");
        serverLevel.playSound(null, getBlockPos(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void powerOn(ServerLevel serverLevel) {
        ServerProtectionCache.updateAllCachesForLevel(serverLevel, "Protector power updated to true");
        serverLevel.playSound(null, getBlockPos(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void sparkEffect() {
        BlockPos pos = getBlockPos();
        ServerLevel serverLevel = getNetwork().getServerLevel();
        if (serverLevel == null) return;

        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        serverLevel.playSound(
                null,
                centerX, centerY, centerZ,
                SoundEvents.REDSTONE_TORCH_BURNOUT,
                SoundSource.BLOCKS,
                0.5F,
                1.2F + serverLevel.random.nextFloat() * 0.4F
        );

        int sparkCount = 2 + serverLevel.random.nextInt(3);
        for (int i = 0; i < sparkCount; i++) {
            // Generate directional velocity vectors pushing outward and upward
            double velX = (serverLevel.random.nextDouble() - 0.5) * 0.4;
            double velY = 0.2 + serverLevel.random.nextDouble() * 0.3; // Always pushes UP out of the block
            double velZ = (serverLevel.random.nextDouble() - 0.5) * 0.4;

            serverLevel.sendParticles(
                    Particles.SPARK,
                    centerX, centerY, centerZ,
                    0, // Must be 0 so the delta coordinates act as velocity
                    velX, velY, velZ,
                    1.0
            );
        }
    }
}

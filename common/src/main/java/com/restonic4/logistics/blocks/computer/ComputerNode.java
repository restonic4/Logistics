package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.blocks.network_connector.NetworkConnectorNode;
import com.restonic4.logistics.experiment.Sounds;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.nodes.ItemNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.networks.types.ItemNetwork;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComputerNode extends EnergyNode {
    public static final long ENERGY_PER_TICK = 1L;

    private String systemName = "DragonOS";
    private String rootPassword = "root";

    private boolean powered = false;
    private boolean installed = false;

    public ComputerNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        super.tick();

        EnergyNetwork network = getNetwork();
        if (network == null) return;

        long energy = network.requestEnergyConsumption(ENERGY_PER_TICK);
        if (energy >= ENERGY_PER_TICK && !isPowered()) {
            setPowered(true);
        } else if (energy < ENERGY_PER_TICK && isPowered()) {
            setPowered(false);
        }

        if (energy < ENERGY_PER_TICK) {
            network.reportEnergyProduction(energy);
        }

        syncBlockState();
    }

    private void syncBlockState() {
        EnergyNetwork network = getNetwork();
        if (network == null) return;

        ServerLevel level = network.getServerLevel();
        BlockPos pos = getBlockPos();
        if (level == null || !level.hasChunkAt(pos)) return;

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ComputerBlock) {
            boolean blockPowered = state.getValue(ComputerBlock.POWERED);
            if (blockPowered != this.powered) {
                level.setBlock(pos, state.setValue(ComputerBlock.POWERED, this.powered), 3);
            }
        }
    }

    public void setPowered(boolean value) {
        if (this.powered == value) return;
        this.powered = value;

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

        syncBlockState();
    }

    public void install(String systemName, String rootPassword) {
        this.systemName = systemName;
        this.rootPassword = rootPassword;
        this.installed = true;
    }

    public boolean isPowered() {
        return this.powered;
    }

    public boolean isInstalled() {
        return this.installed;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getRootPassword() {
        return rootPassword;
    }

    @Override
    public boolean buildScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildScannerTooltip(builder, isSneaking);

        builder.spacer();
        if (installed) {
            builder.keyValue("Hostname", systemName, ChatFormatting.GOLD);
        } else {
            builder.text("OS not found, installation required!", ChatFormatting.RED);
        }

        return true;
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildDebugScannerTooltip(builder, isSneaking);

        builder.spacer();
        builder.keyValue("Energy/tick", String.valueOf(ENERGY_PER_TICK), ChatFormatting.YELLOW);
        builder.keyValue("Powered", isPowered() ? "Yes" : "No", isPowered() ? ChatFormatting.GREEN : ChatFormatting.RED);

        getAdjacentNetwork(EnergyNetwork.class).ifPresent(energy ->
                builder.keyValue("Energy network found", energy.getUUID().toString(), ChatFormatting.YELLOW)
        );

        return true;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putString("systemName", systemName);
        tag.putString("rootPassword", rootPassword);
        tag.putBoolean("powered", powered);
        tag.putBoolean("installed", installed);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        this.systemName = tag.getString("systemName");
        this.rootPassword = tag.getString("rootPassword");
        this.powered = tag.getBoolean("powered");
        this.installed = tag.getBoolean("installed");
    }

    private void powerOff(ServerLevel serverLevel) {
        ServerNetworking.sendToAllInLevel(serverLevel, new ComputerOffPacket(getBlockPos()));
        serverLevel.playSound(null, getBlockPos(), Sounds.COMPUTER_OFF.getSoundEvent(), SoundSource.BLOCKS, 1.0F, 1.0F);
        ComputerLogger.log(serverLevel, getBlockPos(), ComputerLogEntry.Severity.ERROR, "Unexpected system shutdown. Check for power failure.");
    }

    private void powerOn(ServerLevel serverLevel) {
        serverLevel.playSound(null, getBlockPos(), Sounds.COMPUTER_BOOT.getSoundEvent(), SoundSource.BLOCKS, 1.0F, 1.0F);
        serverLevel.playSound(null, getBlockPos(), Sounds.COMPUTER_BOOT_BEEP.getSoundEvent(), SoundSource.BLOCKS, 1.0F, 1.0F);
        ComputerLogger.log(serverLevel, getBlockPos(), ComputerLogEntry.Severity.WARN, "Booting DragonOS...");
    }
}
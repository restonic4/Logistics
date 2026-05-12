package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.nodes.ItemNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

public class ComputerNode extends EnergyNode {
    public static final long ENERGY_PER_TICK = 1L;

    private boolean powered = false;

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

        if (!value) {
            ServerLevel level = getNetwork().getServerLevel();
            if (level != null) {
                ServerNetworking.sendToAllInLevel(level, new ComputerOffPacket(getBlockPos()));
                level.playSound(null, getBlockPos(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }

        syncBlockState();
    }

    public boolean isPowered() {
        return this.powered;
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
}
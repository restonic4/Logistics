package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.networks.nodes.ItemNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;

public class ComputerNode extends ItemNode {
    public static final long ENERGY_PER_TICK = 1L;
    private boolean powered = false;

    public ComputerNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        super.tick();
        powered = getAdjacentNetwork(EnergyNetwork.class)
                .map(energy -> energy.requestEnergyConsumption(ENERGY_PER_TICK) >= ENERGY_PER_TICK)
                .orElse(false);
    }

    public boolean isPowered() {
        return powered;
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildDebugScannerTooltip(builder, isSneaking);

        builder.spacer();
        builder.keyValue("Energy/tick", String.valueOf(ENERGY_PER_TICK), ChatFormatting.YELLOW);
        builder.keyValue("Powered", powered ? "Yes" : "No",
                powered ? ChatFormatting.GREEN : ChatFormatting.RED);

        getAdjacentNetwork(EnergyNetwork.class).ifPresent(energy ->
                builder.keyValue("Energy network found", energy.getUUID().toString(), ChatFormatting.YELLOW)
        );

        return true;
    }
}
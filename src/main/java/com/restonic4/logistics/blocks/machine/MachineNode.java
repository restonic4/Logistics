package com.restonic4.logistics.blocks.machine;

import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class MachineNode extends EnergyNode {
    public static final long CONSUMPTION_PER_TICK = 200L;

    public MachineNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        EnergyNetwork energyNetwork = getNetwork();
        if (energyNetwork == null) return;

        long extracted = energyNetwork.requestEnergyConsumption(CONSUMPTION_PER_TICK);
        float minThreshold = CONSUMPTION_PER_TICK / 2.0f;

        if (extracted >= minThreshold) {
            BlockPos pos = this.getBlockPos();

            float progress = (extracted - minThreshold) / (CONSUMPTION_PER_TICK - minThreshold);
            float volume = 0.1f + (progress * 0.9f);

            energyNetwork.getServerLevel().playSound(
                    null,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.AMETHYST_BLOCK_BREAK,
                    SoundSource.BLOCKS,
                    volume, 1
            );
        } else {
            energyNetwork.reportEnergyProduction(extracted);
        }
    }
}

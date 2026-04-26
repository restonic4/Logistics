package com.restonic4.logistics.blocks.entity;

import com.restonic4.logistics.blocks.BlockEntityRegistry;
import com.restonic4.logistics.energy.EnergyNetwork;
import com.restonic4.logistics.energy.EnergyNode;
import com.restonic4.logistics.energy.EnergyProducer;
import com.restonic4.logistics.energy.OfflineEnergyProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Produces 20 EU/t constantly.
 * In a real mod you'd consume fuel here and call network.recalculateRates()
 * when you run out.
 */
public class GeneratorBlockEntity extends BlockEntity implements EnergyNode, EnergyProducer {
    public static final long PRODUCTION_PER_TICK = 20L;

    @Nullable
    private EnergyNetwork network;
    private long lastProduced = 0;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GENERATOR, pos, state);
    }

    // -------------------------------------------------------------------------
    // IEnergyNode
    // -------------------------------------------------------------------------

    @Override
    public EnergyNetwork getEnergyNetwork() { return network; }

    @Override
    public void setEnergyNetwork(EnergyNetwork network) { this.network = network; }

    @Override
    public BlockPos getEnergyPos() { return getBlockPos(); }

    // -------------------------------------------------------------------------
    // IEnergyProducer
    // -------------------------------------------------------------------------

    @Override
    public long produceEnergy(long budgetAvailable) {
        lastProduced = PRODUCTION_PER_TICK;
        this.checkAndTriggerAutoSave();
        return lastProduced;
    }

    @Override
    public OfflineEnergyProfile getOfflineProducerProfile() {
        return OfflineEnergyProfile.stable(PRODUCTION_PER_TICK);
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("lastProduced", lastProduced);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        lastProduced = tag.getLong("lastProduced");
    }

    // -------------------------------------------------------------------------
    // Getters for debug screen
    // -------------------------------------------------------------------------

    public long getLastProduced() { return lastProduced; }
}
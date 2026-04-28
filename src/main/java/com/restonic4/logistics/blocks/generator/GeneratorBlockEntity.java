package com.restonic4.logistics.blocks.generator;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.base.BaseNetworkBlockEntity;
import com.restonic4.logistics.energy.EnergyProducer;
import com.restonic4.logistics.energy.OfflineEnergyProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Produces 20 EU/t constantly.
 * In a real mod you'd consume fuel here and call network.recalculateRates()
 * when you run out.
 */
public class GeneratorBlockEntity extends BaseNetworkBlockEntity implements EnergyProducer {
    public static final long PRODUCTION_PER_TICK = 20L;

    private long lastProduced = 0;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockRegistry.GENERATOR_BLOCK.getBlockEntityType(), pos, state);
    }

    // -------------------------------------------------------------------------
    // IEnergyProducer
    // -------------------------------------------------------------------------

    @Override
    public long produceEnergy(long budgetAvailable) {
        lastProduced = PRODUCTION_PER_TICK;
        this.setChanged();
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
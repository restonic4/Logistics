package com.restonic4.logistics.blocks.machine;

import com.restonic4.logistics.blocks.BlockRegistry;
import com.restonic4.logistics.blocks.base.BaseNetworkBlockEntity;
import com.restonic4.logistics.energy.EnergyConsumer;
import com.restonic4.logistics.energy.OfflineEnergyProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Consumes 15 EU/t.
 * Tracks whether it was "powered" last tick — you'd use this to gate crafting.
 *
 * It has a small internal buffer (50 EU) so it doesn't immediately stall
 * on a single-tick supply gap (e.g. battery handoff between ticks).
 */
public class MachineBlockEntity extends BaseNetworkBlockEntity implements EnergyConsumer {
    public static final long CONSUMPTION_PER_TICK = 15L;
    public static final long INTERNAL_BUFFER_MAX  = 50L;

    private long internalBuffer = 0;
    private boolean poweredLastTick = false;
    private long lastConsumed = 0;

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(BlockRegistry.MACHINE_BLOCK.getBlockEntityType(), pos, state);
    }

    // -------------------------------------------------------------------------
    // IEnergyConsumer
    // -------------------------------------------------------------------------

    @Override
    public long consumeEnergy(long offered) {
        long canAccept = Math.min(offered, INTERNAL_BUFFER_MAX - internalBuffer);
        internalBuffer += canAccept;

        if (internalBuffer >= CONSUMPTION_PER_TICK) {
            internalBuffer  -= CONSUMPTION_PER_TICK;
            poweredLastTick  = true;
        } else {
            poweredLastTick = false;
        }

        lastConsumed = canAccept;
        this.setChanged();
        return canAccept;
    }

    @Override
    public OfflineEnergyProfile getOfflineConsumerProfile() {
        return OfflineEnergyProfile.stable(CONSUMPTION_PER_TICK);
    }

    @Override
    public long getMaxConsumptionPerTick() { return CONSUMPTION_PER_TICK; }

    @Override
    public boolean needsEnergy() { return internalBuffer < INTERNAL_BUFFER_MAX; }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("internalBuffer", internalBuffer);
        tag.putBoolean("poweredLastTick", poweredLastTick);
        tag.putLong("lastConsumed", lastConsumed);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        internalBuffer = tag.getLong("internalBuffer");
        poweredLastTick = tag.getBoolean("poweredLastTick");
        lastConsumed = tag.getLong("lastConsumed");
    }

    // -------------------------------------------------------------------------
    // Getters for debug screen
    // -------------------------------------------------------------------------

    public long getInternalBuffer() { return internalBuffer; }
    public boolean isPoweredLastTick() { return poweredLastTick; }
    public long getLastConsumed() { return lastConsumed; }
}
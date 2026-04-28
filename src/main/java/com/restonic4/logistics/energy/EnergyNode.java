package com.restonic4.logistics.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Base interface for anything that participates in an energy network.
 * This includes pipes (topology-only) and machines/batteries (producers/consumers).
 *
 * Implement this on your BlockEntity, not on the Block itself.
 */
public interface EnergyNode {

    /**
     * Returns the network this node currently belongs to.
     * May be null if the node hasn't been registered yet (e.g. just placed).
     */
    EnergyNetwork getEnergyNetwork();

    /**
     * Called by EnergyNetworkManager when this node is assigned to a network
     * (on placement, on chunk load, or after a topology rebuild).
     */
    void setEnergyNetwork(EnergyNetwork network);

    /**
     * The world position of this node. Usually just blockEntity.getBlockPos().
     */
    BlockPos getEnergyPos();

    /**
     * Whether this node acts as a cable/pipe — pure topology, no production or consumption.
     * Pipe BlockEntities return true. Machines and batteries return false.
     *
     * The network manager uses this to distinguish "connective tissue" from "endpoints."
     */
    default boolean isPipe() {
        return false;
    }

    /*default boolean checkAndTriggerAutoSave() {
        if (!(this instanceof BlockEntity blockEntity)) {
            return false;
        }

        Level level = blockEntity.getLevel();
        if (level != null && level.getGameTime() % 200 == 0) {
            blockEntity.setChanged();
            return true;
        }
        return false;
    }*/

    default boolean isChunkTicking() {
        if (this instanceof BlockEntity blockEntity) {
            if (blockEntity.getLevel() instanceof ServerLevel serverLevel) {
                return serverLevel.isPositionEntityTicking(blockEntity.getBlockPos());
            }
        }
        return false;
    }
}
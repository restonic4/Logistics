package com.restonic4.logistics.energy;

/**
 * Marker interface for Block classes whose BlockEntity implements IEnergyNode.
 *
 * Why this exists:
 *   PipeBlock.canConnectTo() only receives a BlockState — it cannot call
 *   level.getBlockEntity() to check for IEnergyNode. So machine/battery
 *   blocks need a way to advertise "I accept pipe connections" at the Block
 *   class level, without being a pipe themselves.
 *
 * Usage on your machine block:
 *
 *   public class GeneratorBlock extends Block implements EnergyNodeBlock {
 *       // ... your block code ...
 *   }
 *
 *   public class GeneratorBlockEntity extends BlockEntity implements IEnergyProducer {
 *       // ... your BE code with energy logic ...
 *   }
 *
 * The pipe will visually connect to it, and the network manager will register
 * the BE as a live node when the chunk loads.
 *
 * If you want a machine that does NOT connect to pipes (e.g. a wireless transmitter),
 * simply don't implement this interface — the pipe won't visually extend toward it,
 * and the manager won't add it to any pipe network.
 */
public interface EnergyNodeBlock {
    // Intentionally empty — marker only.
}
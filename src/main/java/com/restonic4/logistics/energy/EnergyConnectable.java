package com.restonic4.logistics.energy;

/**
 * Marker interface for Block classes (not BlockEntities) that participate in the energy network topology.</br>
 * </br>
 * Since pipes don't need a BlockEntity for energy (they carry no state, they're just connections),
 * implementing this on the Block class is enough for the network manager to recognize them as valid topology nodes.</br>
 * </br>
 * If your pipe DOES have a BlockEntity, implement {@link EnergyNode} on the BlockEntity too, both interfaces can coexist.
 */
public interface EnergyConnectable {}
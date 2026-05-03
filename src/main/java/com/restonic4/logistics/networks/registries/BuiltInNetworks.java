package com.restonic4.logistics.networks.registries;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.networks.types.EnergyNetwork;

public class BuiltInNetworks {
    public static final NetworkTypeRegistry.NetworkType<EnergyNetwork> ENERGY_NETWORK = NetworkTypeRegistry.register(Logistics.id("energy"), EnergyNetwork::new);
    public static final NetworkTypeRegistry.NetworkType<EnergyNetwork> ITEM_NETWORK = NetworkTypeRegistry.register(Logistics.id("item"), EnergyNetwork::new);
}

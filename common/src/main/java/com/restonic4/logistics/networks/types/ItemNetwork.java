package com.restonic4.logistics.networks.types;

import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.registry.NetworkTypeRegistry;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;

public class ItemNetwork extends Network {
    public ItemNetwork(NetworkTypeRegistry.NetworkType<?> type, ServerLevel serverLevel) {
        super(type, serverLevel);
    }

    @Override
    public void mergeDataFrom(Network other) {

    }

    @Override
    public void onSplit(Collection<Network> children) {

    }
}

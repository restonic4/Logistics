package com.restonic4.logistics.blocks.protector;

import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.blocks.battery.BatteryNode;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class CreativeProtectorBlock extends BaseNetworkBlock {
    public CreativeProtectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onNodeCreated(NetworkNode node, ServerLevel level, BlockPos pos) {
        if (node instanceof ProtectorNode protectorNode) {
           protectorNode.setCreative(true);
        }
    }
}

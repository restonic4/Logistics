package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.networks.nodes.ItemNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;

public class ComputerNode extends ItemNode {
    public ComputerNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }
}
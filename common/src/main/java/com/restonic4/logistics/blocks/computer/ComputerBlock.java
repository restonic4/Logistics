package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.networking.ServerNetworking;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import com.restonic4.logistics.networks.types.ItemNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

public class ComputerBlock extends BaseNetworkBlock {
    public ComputerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkNode node = NetworkManager.get((ServerLevel) level).getNodeByBlockPos(pos);
            if (node instanceof ComputerNode && node.getNetwork() instanceof ItemNetwork itemNetwork) {
                List<BlockPos> accessors = new ArrayList<>();
                for (NetworkNode networkNode : itemNetwork.getNodeIndex().getAllNodes()) {
                    if (networkNode instanceof AccessorNode) {
                        accessors.add(networkNode.getBlockPos());
                    }
                }
                ServerNetworking.sendToClient(serverPlayer, new ComputerSyncPacket(accessors));
            }
        }

        return InteractionResult.CONSUME;
    }
}
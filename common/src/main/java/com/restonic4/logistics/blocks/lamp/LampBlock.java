package com.restonic4.logistics.blocks.lamp;

import com.restonic4.logistics.blocks.base.BaseNetworkBlock;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class LampBlock extends BaseNetworkBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public LampBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Sneak-use toggles the looping static hum on this lamp (off by default). A plain use is left to
     * the default handling. The new state is flipped on the node, which persists and syncs it.
     */
    @Override
    public InteractionResult onRightClick(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (level instanceof ServerLevel serverLevel) {
            NetworkNode node = NetworkManager.get(serverLevel).getNodeByBlockPos(pos);
            if (node instanceof LampNode lampNode) {
                boolean enabled = !lampNode.isStaticEnabled();
                lampNode.setStaticEnabled(enabled);
                player.displayClientMessage(
                        Component.translatable(enabled ? "logistics.lamp.static.enabled" : "logistics.lamp.static.disabled"),
                        true
                );
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}

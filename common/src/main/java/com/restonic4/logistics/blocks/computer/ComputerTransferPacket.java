package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.filter.ItemFilter;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * A manual transfer request from the computer's Transfer tab. The filter describes the item
 * and how its NBT should be matched; quantities beyond one stack ship as a parcel trail.
 * The heavy lifting lives in {@link ItemTransferService}, shared with automation actions.
 */
public record ComputerTransferPacket(BlockPos computerNode, @Nullable BlockPos from, BlockPos target, int quantity, ItemFilter filter) implements C2SPacket {
    public static final ResourceLocation ID = Logistics.id("computer_transfer");

    /** Hard server-side cap so a malicious client can't request absurd transfers. */
    public static final int MAX_QUANTITY = 1024;

    public ComputerTransferPacket(FriendlyByteBuf buf) {
        this(
                buf.readBlockPos(),
                buf.readBoolean() ? buf.readBlockPos() : null,
                buf.readBlockPos(),
                buf.readInt(),
                ItemFilter.fromBuf(buf)
        );
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        EnergyNetwork energyNetwork = (EnergyNetwork) NetworkManager.get(level).getNetworkByBlockPos(computerNode);
        if (energyNetwork == null) {
            return;
        }

        energyNetwork.execute(() -> {
            ComputerNode computerNodeComp = (ComputerNode) energyNetwork.getNodeIndex().findByBlockPos(computerNode);
            if (computerNodeComp == null || !computerNodeComp.isPowered()) {
                return;
            }

            int clampedQuantity = Mth.clamp(quantity, 1, MAX_QUANTITY);

            ItemTransferService.Result result = ItemTransferService.transfer(
                    level, energyNetwork, computerNode, filter, clampedQuantity, from, target, false, true);

            if (result.success()) {
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        });
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerNode);
        buf.writeBoolean(from != null);
        if (from != null) buf.writeBlockPos(from);
        buf.writeBlockPos(target);
        buf.writeInt(quantity);
        filter.write(buf);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}

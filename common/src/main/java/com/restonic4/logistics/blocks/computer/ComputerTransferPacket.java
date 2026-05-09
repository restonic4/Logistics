package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.pathfinding.Parcel;
import com.restonic4.logistics.networks.types.ItemNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record ComputerTransferPacket(BlockPos from, BlockPos target, int quantity, String query) implements C2SPacket {
    public static final ResourceLocation ID = Logistics.id("computer_transfer");

    public ComputerTransferPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readBlockPos(), buf.readInt(), buf.readUtf());
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        ItemNetwork network = (ItemNetwork) NetworkManager.get((ServerLevel) player.level()).getNetworkByBlockPos(from);
        ResourceLocation itemLocation = new ResourceLocation(query);
        Item item = BuiltInRegistries.ITEM.get(itemLocation);

        if (item == Items.AIR && !query.equals("minecraft:air")) {
            return;
        }

        AccessorNode node = (AccessorNode) network.getNodeIndex().findByBlockPos(from);

        ItemStack itemStack = new ItemStack(item, quantity);
        if (node.consumeItem(itemStack, network.getServerLevel())) {
            Parcel parcel = network.requestParcel(itemStack, from, target);
            if (parcel == null) {
                network.getServerLevel().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                network.getServerLevel().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        } else {
            network.getServerLevel().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(from);
        buf.writeBlockPos(target);
        buf.writeInt(quantity);
        buf.writeUtf(query);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}

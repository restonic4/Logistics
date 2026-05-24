package com.restonic4.logistics.blocks.computer;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.accersor.AccessorNode;
import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.pathfinding.Parcel;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.networks.types.ItemNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ComputerTransferPacket(BlockPos computerNode, @Nullable BlockPos from, BlockPos target, int quantity, String query) implements C2SPacket {
    public static final ResourceLocation ID = Logistics.id("computer_transfer");

    public ComputerTransferPacket(FriendlyByteBuf buf) {
        this(
                buf.readBlockPos(),
                buf.readBoolean() ? buf.readBlockPos() : null,
                buf.readBlockPos(),
                buf.readInt(),
                buf.readUtf()
        );
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        ResourceLocation itemLocation = new ResourceLocation(query);
        Item item = BuiltInRegistries.ITEM.get(itemLocation);

        if (item == Items.AIR && !query.equals("minecraft:air")) {
            return;
        }

        EnergyNetwork energyNetwork = (EnergyNetwork) NetworkManager.get(level).getNetworkByBlockPos(computerNode);
        if (energyNetwork == null) {
            return;
        }

        energyNetwork.execute(() -> {
            ComputerNode computerNodeComp = (ComputerNode) energyNetwork.getNodeIndex().findByBlockPos(computerNode);
            if (computerNodeComp == null || !computerNodeComp.isPowered()) {
                return;
            }

            ItemStack requestedStack = new ItemStack(item, quantity);
            long requirement = (long) quantity * Parcel.ENERGY_PRICE_PER_ITEM;

            AccessorNode sourceNode = null;
            ItemNetwork sourceNetwork = null;

            // ── MANUAL: user picked a specific input accessor ──
            if (from != null) {
                Network network = NetworkManager.get(level).getNetworkByBlockPos(from);
                if (network instanceof ItemNetwork itemNet) {
                    var node = itemNet.getNodeIndex().findByBlockPos(from);
                    if (node instanceof AccessorNode acc) {
                        sourceNode = acc;
                        sourceNetwork = itemNet;
                    }
                }
            }
            // ── AUTO: scan every bridged item network and every accessor ──
            else {
                outer:
                for (var connector : energyNetwork.getNetworkConnectors()) {
                    Network bridged = connector.getBridgedNetwork();
                    if (bridged instanceof ItemNetwork itemNet) {
                        for (var netNode : itemNet.getNodeIndex().getAllNodes()) {
                            if (netNode instanceof AccessorNode acc && !acc.getBlockPos().equals(target)) {
                                List<ItemStack> inv = acc.getVirtualInventory(level);

                                int available = 0;
                                for (int i = 0; i < inv.size(); i++) {
                                    ItemStack stack = inv.get(i);
                                    if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, requestedStack)) {
                                        available += stack.getCount();
                                    }
                                }
                                if (available >= requestedStack.getCount()) {
                                    sourceNode = acc;
                                    sourceNetwork = itemNet;
                                    break outer;
                                }

                            }
                        }
                    }
                }
            }

            if (sourceNode == null || sourceNetwork == null) {
                ComputerLogger.log(level, computerNode,
                        ComputerLogEntry.Severity.WARN,
                        String.format(
                                "Transfer failed: could not find %dx %s in any accessible accessor.",
                                quantity, query
                        ));
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                return;
            }

            // ── Energy gate: no parcel unless there is enough ──
            long energy = energyNetwork.requestEnergyConsumption(requirement);
            if (energy < requirement) {
                energyNetwork.reportEnergyProduction(energy);
                ComputerLogger.log(level, computerNode,
                        ComputerLogEntry.Severity.WARN,
                        String.format(
                                "Transfer failed: not enough energy. Needed %d, found %d.",
                                requirement, energy
                        ));

                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                return;
            }

            // ── Atomic consume + ship ──
            if (sourceNode.consumeItem(requestedStack, level)) {
                Parcel parcel = sourceNetwork.requestParcel(requestedStack, sourceNode.getBlockPos(), target);
                if (parcel == null) {
                    energyNetwork.reportEnergyProduction(energy);
                    ComputerLogger.log(level, computerNode,
                            ComputerLogEntry.Severity.ERROR,
                            String.format(
                                    "Transfer failed: parcel could not be routed (%dx %s → %s).",
                                    quantity, query, target.toShortString()
                            ));
                    level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                } else {
                    ComputerLogger.log(level, computerNode,
                            ComputerLogEntry.Severity.INFO,
                            String.format(
                                    "Dispatched %dx %s from %s to %s.",
                                    quantity, query,
                                    sourceNode.getBlockPos().toShortString(),
                                    target.toShortString()
                            ));
                    level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            } else {
                energyNetwork.reportEnergyProduction(energy);
                ComputerLogger.log(level, computerNode,
                        ComputerLogEntry.Severity.WARN,
                        String.format(
                                "Transfer failed: could not extract %dx %s from source %s.",
                                quantity, query,
                                sourceNode.getBlockPos().toShortString()
                        ));
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
        buf.writeUtf(query);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }
}
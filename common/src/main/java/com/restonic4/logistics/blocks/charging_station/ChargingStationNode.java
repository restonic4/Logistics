package com.restonic4.logistics.blocks.charging_station;

import com.restonic4.logistics.networks.Network;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public class ChargingStationNode extends EnergyNode {
    private ItemStack heldItem = ItemStack.EMPTY;

    public ChargingStationNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    public ItemStack getHeldItem() {
        return heldItem.copy();
    }

    public void setHeldItem(ItemStack stack) {
        this.heldItem = stack.copy();
        Network net = getNetwork();
        if (net != null) net.setDirty();
    }

    @Override
    public void tick() {
        super.tick();

        if (heldItem.isEmpty()) return;
        if (getNetwork() == null) return;

        long current = EnergyItemHelper.getStoredEnergy(heldItem);
        long max = EnergyItemHelper.getMaxEnergy(heldItem);
        if (current >= max) return;

        long extracted = getNetwork().requestEnergyConsumption(1);
        if (extracted > 0) {
            EnergyItemHelper.setStoredEnergy(heldItem, current + extracted);
            getNetwork().setDirty();
        }
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        if (!heldItem.isEmpty()) {
            tag.put("held_item", heldItem.save(new CompoundTag()));
        }
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        if (tag.contains("held_item", Tag.TAG_COMPOUND)) {
            heldItem = ItemStack.of(tag.getCompound("held_item"));
        } else {
            heldItem = ItemStack.EMPTY;
        }
    }
}
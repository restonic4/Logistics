package com.restonic4.logistics.blocks.charging_station;

import com.restonic4.logistics.blocks.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ChargingStationBlockEntity extends BlockEntity {
    private ItemStack renderStack = ItemStack.EMPTY;

    public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(BlockRegistry.CHARGING_STATION_BLOCK.getBlockEntityType(ChargingStationBlockEntity.class), pos, state);
    }

    public void setRenderStack(ItemStack stack) {
        this.renderStack = stack.copy();
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            // Forces the server to sync this block entity data to all nearby clients
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public ItemStack getRenderStack() {
        return this.renderStack;
    }

    // --- Networking & Syncing Boilerplate ---
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("RenderItem", 10)) {
            this.renderStack = ItemStack.of(tag.getCompound("RenderItem"));
        } else {
            this.renderStack = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("RenderItem", this.renderStack.save(new CompoundTag()));
    }
}
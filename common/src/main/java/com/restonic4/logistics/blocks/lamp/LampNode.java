package com.restonic4.logistics.blocks.lamp;

import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class LampNode extends EnergyNode {
    public static final long ENERGY_PER_CYCLE = 1L;
    public static final int TICKS_PER_CYCLE = 20;

    private int tickCounter = 0;
    private boolean lit = false;

    public LampNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        super.tick();

        EnergyNetwork network = getNetwork();
        if (network == null) return;

        if (++tickCounter >= TICKS_PER_CYCLE) {
            tickCounter = 0;
            long consumed = network.requestEnergyConsumption(ENERGY_PER_CYCLE);
            setLit(consumed >= ENERGY_PER_CYCLE);
        }

        syncBlockState();
    }

    /**
     * Pushes the {@link #lit} state into the world, but only when the block's chunk is actually
     * loaded. The node keeps ticking (and consuming energy) regardless of chunk state, so we never
     * force-load a chunk here; we just bring the visible block/light back in sync the moment the
     * chunk becomes available again. {@code setBlock} drives the light engine for us.
     */
    private void syncBlockState() {
        EnergyNetwork network = getNetwork();
        if (network == null) return;

        ServerLevel level = network.getServerLevel();
        BlockPos pos = getBlockPos();
        if (level == null || !level.hasChunkAt(pos)) return;

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LampBlock) {
            boolean blockLit = state.getValue(LampBlock.LIT);
            if (blockLit != this.lit) {
                level.setBlock(pos, state.setValue(LampBlock.LIT, this.lit), 3);
            }
        }
    }

    private void setLit(boolean value) {
        if (this.lit == value) return;
        this.lit = value;
        syncBlockState();
        setNetworkDirty();
    }

    public boolean isLit() { return this.lit; }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildDebugScannerTooltip(builder, isSneaking);

        builder.spacer();
        builder.keyValue("Energy/cycle", ENERGY_PER_CYCLE + " per " + TICKS_PER_CYCLE + "t", ChatFormatting.YELLOW);
        builder.keyValue("Lit", isLit() ? "Yes" : "No", isLit() ? ChatFormatting.GREEN : ChatFormatting.RED);

        return true;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putBoolean("lit", lit);
        tag.putInt("tickCounter", tickCounter);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        this.lit = tag.getBoolean("lit");
        this.tickCounter = tag.getInt("tickCounter");
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        super.writeExtraSyncData(buf);
        buf.writeBoolean(lit);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        super.readExtraSyncData(buf);
        this.lit = buf.readBoolean();
    }
}

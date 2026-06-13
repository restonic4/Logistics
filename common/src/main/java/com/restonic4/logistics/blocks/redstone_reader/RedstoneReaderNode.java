package com.restonic4.logistics.blocks.redstone_reader;

import com.restonic4.logistics.blocks.base.NameIdentifier;
import com.restonic4.logistics.networks.nodes.EnergyNode;
import com.restonic4.logistics.networks.nodes.FacingNode;
import com.restonic4.logistics.networks.tooltip.TooltipBuilder;
import com.restonic4.logistics.networks.types.EnergyNetwork;
import com.restonic4.logistics.registry.NodeTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A pass-through energy node whose single {@code facing} (front) face reads vanilla redstone, the
 * way any redstone component would. The other five faces are normal cable faces (see
 * {@link RedstoneReaderBlock#getAllowedConnections}).
 * <p>
 * The signal is polled in {@link #tick()} only while the chunk is loaded (mirroring {@code LampNode}),
 * so the node never force-loads anything; it simply re-reads the moment the chunk is back. The cached
 * strength is what the computer's redstone trigger evaluates against.
 */
public class RedstoneReaderNode extends EnergyNode implements FacingNode, NameIdentifier {
    @Nullable private Direction facing = null;
    @Nullable private String name = null;

    /** Last redstone strength (0-15) read on the front face. Persisted/synced so triggers stay stable. */
    private int signalStrength = 0;

    public RedstoneReaderNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
    }

    @Override
    public void tick() {
        super.tick();

        EnergyNetwork network = getNetwork();
        if (network == null || facing == null) return;

        ServerLevel level = network.getServerLevel();
        if (level == null || !level.hasChunkAt(getBlockPos())) return;

        BlockPos frontPos = getBlockPos().relative(facing);
        int read = level.getSignal(frontPos, facing);
        if (read != signalStrength) {
            this.signalStrength = read;
            setNetworkDirty();
        }
    }

    /** Last redstone strength (0-15) read on the front face. */
    public int getSignalStrength() { return signalStrength; }

    /** Whether the front face currently sees any redstone power. */
    public boolean isPowered() { return signalStrength > 0; }

    @Override
    public void setFacing(@NotNull Direction facing) {
        this.onFacingChange(this.facing, facing, this);
        this.facing = facing;
    }

    @Override
    @Nullable public Direction getFacing() {
        return facing;
    }

    @Override
    public void setName(@NotNull String name) {
        this.onNameChange(this.name, name, this);
        this.name = name;
    }

    @Override
    @Nullable public String getName() {
        return name;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        this.saveFacing(tag);
        this.saveName(tag);
        tag.putInt("signal", signalStrength);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        this.loadFacing(tag);
        this.loadName(tag);
        this.signalStrength = tag.getInt("signal");
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        super.writeExtraSyncData(buf);
        this.writeFacing(buf);
        this.writeName(buf);
        buf.writeVarInt(signalStrength);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        super.readExtraSyncData(buf);
        this.readFacing(buf);
        this.readName(buf);
        this.signalStrength = buf.readVarInt();
    }

    @Override
    public boolean buildScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        boolean added = super.buildScannerTooltip(builder, isSneaking);
        boolean nameAddition = this.buildNameScannerTooltip(builder, isSneaking);
        return added || nameAddition;
    }

    @Override
    public boolean buildDebugScannerTooltip(TooltipBuilder builder, boolean isSneaking) {
        super.buildDebugScannerTooltip(builder, isSneaking);

        builder.spacer();
        builder.keyValue("Facing", facing != null ? facing.getName() : "unset", ChatFormatting.YELLOW);
        builder.keyValue("Name", name != null ? name : "unset", ChatFormatting.YELLOW);
        builder.keyValue("Signal", String.valueOf(signalStrength), isPowered() ? ChatFormatting.GREEN : ChatFormatting.RED);

        return true;
    }
}

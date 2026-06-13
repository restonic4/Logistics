package com.restonic4.logistics.blocks.network_switch;

import com.restonic4.logistics.blocks.base.NameIdentifier;
import com.restonic4.logistics.networks.nodes.EnergyNode;
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

import java.util.StringJoiner;

/**
 * A pass-through energy node that just carries identity/name for a {@link NetworkSwitchBlock}; the
 * actual per-face connectivity is blockstate-driven (see the block), so there is no extra
 * connectivity state to persist here.
 */
public class NetworkSwitchNode extends EnergyNode implements NameIdentifier {
    @Nullable private String name = null;

    public NetworkSwitchNode(NodeTypeRegistry.NetworkNodeType<?> type, BlockPos blockPos) {
        super(type, blockPos);
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
        this.saveName(tag);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        this.loadName(tag);
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        super.writeExtraSyncData(buf);
        this.writeName(buf);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        super.readExtraSyncData(buf);
        this.readName(buf);
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
        builder.keyValue("Name", name != null ? name : "unset", ChatFormatting.YELLOW);

        EnergyNetwork network = getNetwork();
        ServerLevel level = network != null ? network.getServerLevel() : null;
        if (level != null && level.hasChunkAt(getBlockPos())) {
            StringJoiner enabled = new StringJoiner(", ");
            for (Direction dir : Direction.values()) {
                if (NetworkSwitchBlock.isFaceEnabled(level, getBlockPos(), dir)) {
                    enabled.add(dir.getName());
                }
            }
            String value = enabled.length() == 0 ? "none" : enabled.toString();
            builder.keyValue("Enabled faces", value, ChatFormatting.YELLOW);
        }

        return true;
    }
}

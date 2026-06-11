package com.restonic4.logistics.blocks.computer.automation;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.Logistics;
import com.restonic4.logistics.blocks.computer.ComputerLogEntry;
import com.restonic4.logistics.blocks.computer.ComputerLogger;
import com.restonic4.logistics.blocks.computer.ComputerNode;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.Trigger;
import com.restonic4.logistics.networking.C2SPacket;
import com.restonic4.logistics.networks.NetworkManager;
import com.restonic4.logistics.networks.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends the Triggers tab's edited configuration to the server, where it replaces the
 * computer's trigger setup. Triggers travel as NBT (the same format used for disk
 * storage), so client and server can never disagree on the wire format.
 */
public class TriggerSavePacket implements C2SPacket {
    public static final ResourceLocation ID = Logistics.id("trigger_save");

    /** Sanity caps so a malicious/broken client can't flood a node with config. */
    public static final int MAX_TRIGGERS = 64;
    public static final int MAX_ACTIONS_PER_TRIGGER = 64;

    private static final String TAG_TRIGGERS = "triggers";

    private final BlockPos computerNodePos;
    private final ListTag triggersTag;

    public TriggerSavePacket(BlockPos computerNodePos, List<Trigger> triggers) {
        this.computerNodePos = computerNodePos;
        this.triggersTag = new ListTag();
        for (Trigger trigger : triggers) {
            this.triggersTag.add(trigger.save());
        }
    }

    public TriggerSavePacket(FriendlyByteBuf buf) {
        this.computerNodePos = buf.readBlockPos();
        CompoundTag wrapper = buf.readNbt();
        this.triggersTag = wrapper != null ? wrapper.getList(TAG_TRIGGERS, Tag.TAG_COMPOUND) : new ListTag();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(computerNodePos);
        CompoundTag wrapper = new CompoundTag();
        wrapper.put(TAG_TRIGGERS, triggersTag);
        buf.writeNbt(wrapper);
    }

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player) {
        NetworkNode node = NetworkManager.get(player.serverLevel()).getNodeByBlockPos(computerNodePos);
        if (!(node instanceof ComputerNode computer)) return;

        if (triggersTag.size() > MAX_TRIGGERS) {
            Constants.LOG.warn("Rejected trigger save from {}: too many triggers ({})",
                    player.getGameProfile().getName(), triggersTag.size());
            return;
        }

        List<Trigger> triggers = new ArrayList<>(triggersTag.size());
        try {
            for (int i = 0; i < triggersTag.size(); i++) {
                Trigger trigger = Trigger.createFromTag(triggersTag.getCompound(i));
                if (trigger.getActions().size() > MAX_ACTIONS_PER_TRIGGER) {
                    Constants.LOG.warn("Rejected trigger save from {}: too many actions ({})",
                            player.getGameProfile().getName(), trigger.getActions().size());
                    return;
                }
                triggers.add(trigger);
            }
        } catch (Exception e) {
            Constants.LOG.warn("Rejected malformed trigger save from {}: {}",
                    player.getGameProfile().getName(), e.getMessage());
            return;
        }

        computer.getTriggerManager().replaceTriggers(triggers);
        computer.setNetworkDirty();

        ComputerLogger.log(player.serverLevel(), computerNodePos, ComputerLogEntry.Severity.INFO,
                "Automation rules updated by " + player.getGameProfile().getName()
                        + " (" + triggers.size() + " trigger" + (triggers.size() == 1 ? "" : "s") + ")");
    }
}

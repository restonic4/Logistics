package com.restonic4.logistics.blocks.computer.automation.triggers.actions;

import com.restonic4.logistics.blocks.computer.ComputerLogEntry;
import com.restonic4.logistics.blocks.computer.ComputerLogger;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.ActionExecutionContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.ExecuteResult;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerAction;
import com.restonic4.logistics.blocks.computer.automation.triggers.core.TriggerContext;
import com.restonic4.logistics.blocks.computer.automation.triggers.registry.ActionRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Writes a message to the computer's log (visible in the Log tab). Useful both as a
 * user-facing notification ("Low energy alarm fired") and for debugging trigger setups.
 */
public class LogMessageAction extends TriggerAction {
    private static final String TAG_MESSAGE = "message";
    private static final String TAG_SEVERITY = "severity";

    private String message = "Trigger fired";
    private ComputerLogEntry.Severity severity = ComputerLogEntry.Severity.INFO;

    public LogMessageAction() {
        super(ActionRegistry.LOG_MESSAGE);
    }

    public String getLogMessage() { return message; }
    public void setLogMessage(String message) {
        this.message = message == null ? "" : message;
    }

    public ComputerLogEntry.Severity getSeverity() { return severity; }
    public void setSeverity(ComputerLogEntry.Severity severity) { this.severity = severity; }

    @Override
    public ExecuteResult execute(TriggerContext ctx, ActionExecutionContext runCtx) {
        ComputerLogger.log(ctx.getLevel(), ctx.getBlockPos(), severity, message);
        return ExecuteResult.SUCCESS;
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putString(TAG_MESSAGE, message);
        tag.putString(TAG_SEVERITY, severity.name());
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        this.message = tag.getString(TAG_MESSAGE);
        this.severity = ComputerLogEntry.Severity.valueOf(tag.getString(TAG_SEVERITY));
    }

    @Override
    protected void writeExtraSyncData(FriendlyByteBuf buf) {
        buf.writeUtf(message);
        buf.writeEnum(severity);
    }

    @Override
    protected void readExtraSyncData(FriendlyByteBuf buf) {
        this.message = buf.readUtf();
        this.severity = buf.readEnum(ComputerLogEntry.Severity.class);
    }
}

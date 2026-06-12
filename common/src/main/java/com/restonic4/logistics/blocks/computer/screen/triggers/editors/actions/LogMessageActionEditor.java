package com.restonic4.logistics.blocks.computer.screen.triggers.editors.actions;

import com.restonic4.logistics.blocks.computer.ComputerLogEntry;
import com.restonic4.logistics.blocks.computer.automation.triggers.actions.LogMessageAction;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.ActionEditor;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.EditorBuilder;
import net.minecraft.network.chat.Component;

public class LogMessageActionEditor implements ActionEditor<LogMessageAction> {
    @Override
    public void buildConfig(LogMessageAction log, EditorBuilder b) {
        b.enumDropdown(0, ComputerLogEntry.Severity.values(), log.getSeverity(),
                severity -> Component.literal(severity.name()), log::setSeverity);
        b.editBox(1, log.getLogMessage(), 128, log::setLogMessage);
        b.endRow();
    }
}

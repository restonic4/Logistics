package com.restonic4.logistics.blocks.computer.screen.triggers.editors.actions;

import com.restonic4.logistics.blocks.computer.automation.triggers.actions.SendItemsAction;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.ActionEditor;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.EditorBuilder;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.ItemFilterRows;
import net.minecraft.network.chat.Component;

public class SendItemsActionEditor implements ActionEditor<SendItemsAction> {
    @Override
    public void buildConfig(SendItemsAction action, EditorBuilder b) {
        b.columnLabels(
                Component.translatable("screen.logistics.computer.tab.triggers.source"),
                Component.translatable("screen.logistics.computer.tab.triggers.target"));
        ItemFilterRows.accessorTargetDropdown(b, 0, action.getSource(),
                Component.translatable("screen.logistics.computer.tab.transfer.auto"));
        ItemFilterRows.accessorDropdown(b, 1, action.getTargetAccessorId(), action::setTargetAccessorId);
        b.endRow();

        b.columnLabels(
                Component.translatable("screen.logistics.computer.tab.triggers.send_mode"),
                Component.translatable("screen.logistics.computer.tab.triggers.amount"));
        b.enumDropdown(0, SendItemsAction.Mode.values(), action.getSendMode(),
                mode -> Component.translatable("screen.logistics.computer.tab.triggers.send_mode." + mode.name().toLowerCase()),
                action::setSendMode);
        b.number(1, action.getAmount(), 1, SendItemsAction.MAX_AMOUNT, 0,
                value -> action.setAmount((int) (double) value));
        b.endRow();

        b.columnLabels(
                Component.translatable("screen.logistics.computer.tab.triggers.log_transfers"),
                Component.translatable("screen.logistics.computer.tab.triggers.stop_on_failure"));
        b.toggle(0, action.isLogTransfers(), action::setLogTransfers);
        b.toggle(1, action.isStopOnFailure(), action::setStopOnFailure);
        b.endRow();

        ItemFilterRows.build(action.getFilter(), b);
    }
}

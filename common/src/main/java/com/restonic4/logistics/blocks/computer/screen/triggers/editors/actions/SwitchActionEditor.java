package com.restonic4.logistics.blocks.computer.screen.triggers.editors.actions;

import com.restonic4.logistics.blocks.computer.automation.triggers.actions.SwitchAction;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.ActionEditor;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.EditorBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

public class SwitchActionEditor implements ActionEditor<SwitchAction> {
    @Override
    public void buildConfig(SwitchAction action, EditorBuilder b) {
        b.columnLabels(
                Component.translatable("screen.logistics.computer.tab.triggers.switch"),
                Component.translatable("screen.logistics.computer.tab.triggers.operation"));
        b.nodeDropdown(0, action.getTarget(), EditorBuilder.networkSwitches(),
                Component.translatable("screen.logistics.computer.tab.triggers.all_switches"));
        b.enumDropdown(1, SwitchAction.Operation.values(), action.getOperation(),
                op -> Component.translatable("screen.logistics.computer.tab.triggers.operation." + op.name().toLowerCase()),
                action::setOperation);
        b.endRow();

        b.columnLabels(
                Component.translatable("screen.logistics.computer.tab.triggers.all_faces"),
                Component.translatable("screen.logistics.computer.tab.triggers.face"));
        b.toggle(0, action.isAllFaces(), action::setAllFaces);
        b.enumDropdown(1, Direction.values(), action.getFace(),
                dir -> Component.translatable("screen.logistics.computer.tab.triggers.face." + dir.getName()),
                action::setFace);
        b.endRow();
    }
}

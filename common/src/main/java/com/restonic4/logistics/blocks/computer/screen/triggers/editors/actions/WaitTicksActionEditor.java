package com.restonic4.logistics.blocks.computer.screen.triggers.editors.actions;

import com.restonic4.logistics.blocks.computer.automation.triggers.actions.WaitTicksAction;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.ActionEditor;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.EditorBuilder;

public class WaitTicksActionEditor implements ActionEditor<WaitTicksAction> {
    @Override
    public void buildConfig(WaitTicksAction wait, EditorBuilder b) {
        b.number(0, wait.getWaitTicks(), 0, 72000, 0, v -> wait.setWaitTicks(v.intValue()));
        b.endRow();
    }
}

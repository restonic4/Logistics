package com.restonic4.logistics.blocks.computer.screen.triggers.editors.actions;

import com.restonic4.logistics.blocks.computer.automation.triggers.actions.StopAudioAction;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.ActionEditor;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.EditorBuilder;

public class StopAudioActionEditor implements ActionEditor<StopAudioAction> {
    @Override
    public void buildConfig(StopAudioAction stop, EditorBuilder b) {
        b.stationDropdown(0, stop.getTarget());
        b.endRow();
    }
}

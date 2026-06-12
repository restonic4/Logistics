package com.restonic4.logistics.blocks.computer.screen.triggers.editors.actions;

import com.restonic4.logistics.blocks.computer.automation.triggers.actions.WaitAudioAction;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.ActionEditor;
import com.restonic4.logistics.blocks.computer.screen.triggers.editors.EditorBuilder;

public class WaitAudioActionEditor implements ActionEditor<WaitAudioAction> {
    @Override
    public void buildConfig(WaitAudioAction waitAudio, EditorBuilder b) {
        b.stationDropdown(0, waitAudio.getTarget());
        b.endRow();
    }
}
